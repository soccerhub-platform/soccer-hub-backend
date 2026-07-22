package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.enums.ClientStatus;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.ClientStudentRelation;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ClientStudentRelationRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationOutput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceDetailsOutput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceListQuery;
import kz.edu.soccerhub.common.dto.client.ClientWorkspacePageOutput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceUpdateCommand;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceStatusCommand;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryQueryInput;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.ClientStudentRelationPort;
import kz.edu.soccerhub.common.port.ClientWorkspacePort;
import kz.edu.soccerhub.common.port.PaymentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientWorkspaceService implements ClientWorkspacePort {

    private final ClientRepository clientRepository;
    private final ClientStudentRelationRepository relationRepository;
    private final ContractRepository contractRepository;
    private final PaymentPort paymentPort;
    private final ClientStudentRelationPort relationPort;

    @Override
    @Transactional(readOnly = true)
    public UUID getClientBranchId(UUID clientId) {
        return findClient(clientId).getBranchId();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientWorkspacePageOutput getClients(UUID branchId, ClientWorkspaceListQuery query, Pageable pageable) {
        String search = query == null ? null : query.search();
        Page<Client> page = clientRepository.search(
                branchId, normalizeSearch(search), parseUuid(search), Pageable.unpaged()
        );
        List<Client> clients = page.getContent();
        if (clients.isEmpty()) {
            return emptyPage(pageable);
        }

        Set<UUID> clientIds = clients.stream().map(Client::getId).collect(Collectors.toSet());
        Map<UUID, Long> studentsCount = relationRepository.findByClientIdInAndEndedAtIsNull(clientIds).stream()
                .collect(Collectors.groupingBy(ClientStudentRelation::getClientId, Collectors.counting()));
        Map<UUID, List<Contract>> contractsByClient = contractRepository.findByClientIdIn(clientIds).stream()
                .collect(Collectors.groupingBy(Contract::getClientId));
        Map<UUID, ContractPaymentSummaryOutput> payments = loadPayments(
                contractsByClient.values().stream().flatMap(Collection::stream).toList()
        );

        List<ClientWorkspacePageOutput.Item> items = clients.stream()
                .map(client -> toListItem(
                        client,
                        studentsCount.getOrDefault(client.getId(), 0L).intValue(),
                        contractsByClient.getOrDefault(client.getId(), List.of()),
                        payments
                ))
                .filter(item -> matchesFilters(item, query))
                .sorted(listComparator(pageable.getSort()))
                .toList();

        int size = pageable.isPaged() ? pageable.getPageSize() : Math.max(items.size(), 1);
        int requestedPage = pageable.isPaged() ? pageable.getPageNumber() : 0;
        int totalPages = items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / size);
        int resolvedPage = totalPages == 0 ? 0 : Math.min(requestedPage, totalPages - 1);
        int fromIndex = Math.min(resolvedPage * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());

        return new ClientWorkspacePageOutput(
                new ArrayList<>(items.subList(fromIndex, toIndex)),
                items.size(), totalPages, resolvedPage, size, summarizePage(items)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ClientWorkspaceDetailsOutput getClient(UUID clientId) {
        Client client = findClient(clientId);
        List<Contract> contracts = contractRepository.findByClientId(clientId);
        List<Contract> activeContracts = contracts.stream().filter(this::isActiveContract).toList();
        MoneySummary money = summarizeMoney(activeContracts, loadPayments(activeContracts));
        List<ClientStudentRelationOutput> students = relationPort.getClientStudents(clientId);

        return new ClientWorkspaceDetailsOutput(
                new ClientWorkspaceDetailsOutput.ClientBlock(
                        client.getId(), client.getBranchId(), fullName(client), client.getFirstName(), client.getLastName(),
                        client.getPhone(), client.getEmail(),
                        client.getStatus() == null ? null : client.getStatus().name(), client.getSource(),
                        client.getComments(), client.getCreatedAt()
                ),
                new ClientWorkspaceDetailsOutput.SummaryBlock(
                        (int) students.stream().filter(ClientStudentRelationOutput::active).count(),
                        activeContracts.size(), contracts.size(), toMoneyBlock(money)
                ),
                students,
                capabilities(client.getStatus(), !activeContracts.isEmpty())
        );
    }

    @Override
    @Transactional
    public ClientWorkspaceDetailsOutput create(ClientWorkspaceCreateCommand command) {
        validatePhoneAvailable(command.phone(), null);
        Client client = clientRepository.save(Client.builder()
                .id(UUID.randomUUID())
                .branchId(command.branchId())
                .firstName(required(command.firstName(), "firstName"))
                .lastName(trimToNull(command.lastName()))
                .phone(trimToNull(command.phone()))
                .email(normalizeEmail(command.email()))
                .source(trimToNull(command.source()))
                .comments(trimToNull(command.comments()))
                .status(ClientStatus.NEW)
                .build());
        return getClient(client.getId());
    }

    @Override
    @Transactional
    public ClientWorkspaceDetailsOutput update(ClientWorkspaceUpdateCommand command) {
        Client client = findClient(command.clientId());
        validatePhoneAvailable(command.phone(), client.getId());
        client.setFirstName(required(command.firstName(), "firstName"));
        client.setLastName(trimToNull(command.lastName()));
        client.setPhone(trimToNull(command.phone()));
        client.setEmail(normalizeEmail(command.email()));
        client.setSource(trimToNull(command.source()));
        client.setComments(trimToNull(command.comments()));
        return getClient(client.getId());
    }

    @Override
    @Transactional
    public ClientWorkspaceDetailsOutput changeStatus(ClientWorkspaceStatusCommand command) {
        Client client = findClient(command.clientId());
        ClientStatus targetStatus;
        try {
            targetStatus = ClientStatus.valueOf(
                    required(command.status(), "status").toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unsupported client status", command.status());
        }
        validateStatusTransition(client.getStatus(), targetStatus);
        client.setStatus(targetStatus);
        return getClient(client.getId());
    }

    private ClientWorkspaceDetailsOutput.CapabilitiesBlock capabilities(
            ClientStatus status,
            boolean hasActiveContracts
    ) {
        boolean operational = status != ClientStatus.INACTIVE;
        return new ClientWorkspaceDetailsOutput.CapabilitiesBlock(
                true,
                operational,
                operational,
                hasActiveContracts,
                status != ClientStatus.ACTIVE,
                status == ClientStatus.ACTIVE,
                status != ClientStatus.INACTIVE
        );
    }

    private void validateStatusTransition(ClientStatus current, ClientStatus target) {
        if (current == target) {
            throw new ConflictException("Client already has requested status", "CLIENT_STATUS_UNCHANGED",
                    Map.of("status", target.name()));
        }
        boolean allowed = target == ClientStatus.ACTIVE
                || target == ClientStatus.INACTIVE
                || (target == ClientStatus.PAUSED && current == ClientStatus.ACTIVE);
        if (!allowed) {
            throw new ConflictException("Client status transition is not allowed", "CLIENT_STATUS_TRANSITION_NOT_ALLOWED",
                    Map.of("currentStatus", current == null ? "UNKNOWN" : current.name(), "targetStatus", target.name()));
        }
    }

    private ClientWorkspacePageOutput.Item toListItem(
            Client client,
            int studentsCount,
            List<Contract> contracts,
            Map<UUID, ContractPaymentSummaryOutput> payments
    ) {
        List<Contract> activeContracts = contracts.stream().filter(this::isActiveContract).toList();
        MoneySummary money = summarizeMoney(activeContracts, payments);
        return new ClientWorkspacePageOutput.Item(
                client.getId(), fullName(client), client.getPhone(), client.getEmail(),
                client.getStatus() == null ? null : client.getStatus().name(), studentsCount, activeContracts.size(),
                money.paidAmount(), money.outstandingAmount(), money.currency(), money.mixedCurrencies(), money.lastPaidAt(),
                paymentStatus(activeContracts, money)
        );
    }

    private Map<UUID, ContractPaymentSummaryOutput> loadPayments(List<Contract> contracts) {
        if (contracts.isEmpty()) return Map.of();
        return paymentPort.getContractPaymentSummaries(contracts.stream()
                .map(contract -> new ContractPaymentSummaryQueryInput(contract.getId(), amount(contract)))
                .toList());
    }

    private MoneySummary summarizeMoney(List<Contract> contracts, Map<UUID, ContractPaymentSummaryOutput> payments) {
        if (contracts.isEmpty()) return MoneySummary.empty();
        Set<UUID> contractIds = contracts.stream().map(Contract::getId).collect(Collectors.toSet());
        List<ContractPaymentSummaryOutput> relevantPayments = payments.entrySet().stream()
                .filter(entry -> contractIds.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .toList();
        Set<String> currencies = contracts.stream().map(Contract::getCurrency).filter(Objects::nonNull)
                .map(value -> value.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        if (currencies.size() > 1) {
            return new MoneySummary(null, null, null, null, null, true, latestPayment(relevantPayments));
        }
        return new MoneySummary(
                contracts.stream().map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                sum(relevantPayments, ContractPaymentSummaryOutput::paidAmount),
                sum(relevantPayments, ContractPaymentSummaryOutput::outstandingAmount),
                sum(relevantPayments, ContractPaymentSummaryOutput::overpaidAmount),
                currencies.stream().findFirst().orElse("KZT"), false, latestPayment(relevantPayments)
        );
    }

    private BigDecimal sum(Collection<ContractPaymentSummaryOutput> values, Function<ContractPaymentSummaryOutput, BigDecimal> extractor) {
        return values.stream().map(extractor).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTime latestPayment(Collection<ContractPaymentSummaryOutput> values) {
        return values.stream().map(ContractPaymentSummaryOutput::lastPaidAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
    }

    private boolean isActiveContract(Contract contract) {
        if (contract.getStatus() == ContractStatus.CANCELLED || contract.getStatus() == ContractStatus.EXPIRED) return false;
        return contract.getEndDate() == null || !contract.getEndDate().isBefore(LocalDate.now());
    }

    private ClientWorkspaceDetailsOutput.MoneyBlock toMoneyBlock(MoneySummary money) {
        return new ClientWorkspaceDetailsOutput.MoneyBlock(
                money.paidAmount(), money.outstandingAmount(), money.overpaidAmount(), money.currency(),
                money.mixedCurrencies(), money.lastPaidAt()
        );
    }

    private Client findClient(UUID id) {
        return clientRepository.findById(id).orElseThrow(() -> new NotFoundException("Client not found", id));
    }

    private void validatePhoneAvailable(String phone, UUID currentClientId) {
        String normalized = trimToNull(phone);
        if (normalized == null) {
            return;
        }
        clientRepository.findByPhone(normalized)
                .filter(existing -> !Objects.equals(existing.getId(), currentClientId))
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "Client phone is already in use",
                            "CLIENT_PHONE_ALREADY_EXISTS",
                            Map.of("clientId", existing.getId(), "phone", normalized)
                    );
                });
    }

    private String required(String value, String field) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BadRequestException("Required client field is missing", field);
        }
        return normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private BigDecimal amount(Contract contract) {
        return contract.getAmount() == null ? BigDecimal.ZERO : contract.getAmount();
    }

    private String normalizeSearch(String search) {
        return search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String fullName(Client client) {
        return ((client.getFirstName() == null ? "" : client.getFirstName().trim()) + " "
                + (client.getLastName() == null ? "" : client.getLastName().trim())).trim();
    }

    private ClientWorkspacePageOutput emptyPage(Pageable pageable) {
        int size = pageable.isPaged() ? pageable.getPageSize() : 0;
        return new ClientWorkspacePageOutput(
                List.of(), 0, 0, 0, size,
                new ClientWorkspacePageOutput.Summary(0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, "KZT", false)
        );
    }

    private boolean matchesFilters(ClientWorkspacePageOutput.Item item, ClientWorkspaceListQuery query) {
        if (query == null) return true;
        Set<String> statuses = query.statuses() == null ? Set.of() : query.statuses().stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        Set<String> supportedStatuses = java.util.Arrays.stream(ClientStatus.values())
                .map(Enum::name).collect(Collectors.toSet());
        if (!supportedStatuses.containsAll(statuses)) {
            throw new BadRequestException("Unsupported client status filter", statuses);
        }
        if (!statuses.isEmpty() && !statuses.contains(item.status())) return false;

        String students = normalizedFilter(query.students());
        if ("WITH_STUDENTS".equals(students) && item.studentsCount() == 0) return false;
        if ("WITHOUT_STUDENTS".equals(students) && item.studentsCount() > 0) return false;
        validateFilter(students, Set.of("ALL", "WITH_STUDENTS", "WITHOUT_STUDENTS"), "students");

        String contracts = normalizedFilter(query.contracts());
        if ("ACTIVE".equals(contracts) && item.activeContractsCount() == 0) return false;
        if ("NO_ACTIVE".equals(contracts) && item.activeContractsCount() > 0) return false;
        validateFilter(contracts, Set.of("ALL", "ACTIVE", "NO_ACTIVE"), "contracts");

        String payment = normalizedFilter(query.payment());
        validateFilter(payment, Set.of(
                "ALL", "PAID", "DEBT", "UNPAID", "PARTIALLY_PAID", "NO_CONTRACT", "NO_AMOUNT", "MIXED_CURRENCIES"
        ), "payment");
        if ("DEBT".equals(payment)) {
            return "UNPAID".equals(item.paymentStatus()) || "PARTIALLY_PAID".equals(item.paymentStatus());
        }
        return "ALL".equals(payment) || payment.equals(item.paymentStatus());
    }

    private String normalizedFilter(String value) {
        return value == null || value.isBlank() ? "ALL" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void validateFilter(String value, Set<String> supported, String field) {
        if (!supported.contains(value)) {
            throw new BadRequestException("Unsupported client list filter", Map.of("field", field, "value", value));
        }
    }

    private Comparator<ClientWorkspacePageOutput.Item> listComparator(Sort sort) {
        Sort.Order order = sort == null ? null : sort.stream().findFirst().orElse(null);
        String property = order == null ? "fullName" : order.getProperty();
        boolean descending = order != null && order.isDescending();
        Comparator<BigDecimal> amountComparator = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
        Comparator<LocalDateTime> dateComparator = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
        Comparator<ClientWorkspacePageOutput.Item> comparator = switch (property) {
            case "status" -> Comparator.comparing(ClientWorkspacePageOutput.Item::status,
                    Comparator.nullsLast(descending ? String.CASE_INSENSITIVE_ORDER.reversed() : String.CASE_INSENSITIVE_ORDER));
            case "studentsCount" -> descending
                    ? Comparator.comparingInt(ClientWorkspacePageOutput.Item::studentsCount).reversed()
                    : Comparator.comparingInt(ClientWorkspacePageOutput.Item::studentsCount);
            case "activeContractsCount" -> descending
                    ? Comparator.comparingInt(ClientWorkspacePageOutput.Item::activeContractsCount).reversed()
                    : Comparator.comparingInt(ClientWorkspacePageOutput.Item::activeContractsCount);
            case "paidAmount" -> Comparator.comparing(ClientWorkspacePageOutput.Item::paidAmount,
                    Comparator.nullsLast(amountComparator));
            case "outstandingAmount" -> Comparator.comparing(ClientWorkspacePageOutput.Item::outstandingAmount,
                    Comparator.nullsLast(amountComparator));
            case "lastPaidAt" -> Comparator.comparing(ClientWorkspacePageOutput.Item::lastPaidAt,
                    Comparator.nullsLast(dateComparator));
            case "fullName", "firstName" -> Comparator.comparing(ClientWorkspacePageOutput.Item::fullName,
                    descending ? String.CASE_INSENSITIVE_ORDER.reversed() : String.CASE_INSENSITIVE_ORDER);
            default -> throw new BadRequestException("Unsupported client list sort", property);
        };
        return comparator.thenComparing(ClientWorkspacePageOutput.Item::fullName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ClientWorkspacePageOutput.Item::id);
    }

    private String paymentStatus(List<Contract> activeContracts, MoneySummary money) {
        if (activeContracts.isEmpty()) return "NO_CONTRACT";
        if (money.mixedCurrencies()) return "MIXED_CURRENCIES";
        if (money.contractAmount().compareTo(BigDecimal.ZERO) <= 0) return "NO_AMOUNT";
        if (money.outstandingAmount().compareTo(BigDecimal.ZERO) <= 0) return "PAID";
        if (money.paidAmount().compareTo(BigDecimal.ZERO) > 0) return "PARTIALLY_PAID";
        return "UNPAID";
    }

    private ClientWorkspacePageOutput.Summary summarizePage(List<ClientWorkspacePageOutput.Item> items) {
        boolean mixedCurrencies = items.stream().anyMatch(ClientWorkspacePageOutput.Item::mixedCurrencies)
                || items.stream().map(ClientWorkspacePageOutput.Item::currency).filter(Objects::nonNull).distinct().count() > 1;
        BigDecimal paid = mixedCurrencies ? null : items.stream().map(ClientWorkspacePageOutput.Item::paidAmount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = mixedCurrencies ? null : items.stream().map(ClientWorkspacePageOutput.Item::outstandingAmount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = mixedCurrencies ? null : items.stream().map(ClientWorkspacePageOutput.Item::currency)
                .filter(Objects::nonNull).findFirst().orElse("KZT");
        return new ClientWorkspacePageOutput.Summary(
                items.size(), items.stream().filter(item -> "ACTIVE".equals(item.status())).count(),
                items.stream().mapToLong(ClientWorkspacePageOutput.Item::studentsCount).sum(),
                items.stream().mapToLong(ClientWorkspacePageOutput.Item::activeContractsCount).sum(),
                paid, outstanding, currency, mixedCurrencies
        );
    }

    private record MoneySummary(
            BigDecimal contractAmount,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            BigDecimal overpaidAmount,
            String currency,
            boolean mixedCurrencies,
            LocalDateTime lastPaidAt
    ) {
        private static MoneySummary empty() {
            return new MoneySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "KZT", false, null);
        }
    }
}
