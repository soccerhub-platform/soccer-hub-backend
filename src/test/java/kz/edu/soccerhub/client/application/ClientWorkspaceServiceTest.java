package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.enums.ClientStatus;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ClientStudentRelationRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationOutput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceListQuery;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceStatusCommand;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceUpdateCommand;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.common.port.ClientStudentRelationPort;
import kz.edu.soccerhub.common.port.PaymentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientWorkspaceServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private ClientStudentRelationRepository relationRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private PaymentPort paymentPort;
    @Mock private ClientStudentRelationPort relationPort;

    private ClientWorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new ClientWorkspaceService(
                clientRepository, relationRepository, contractRepository, paymentPort, relationPort
        );
    }

    @Test
    void detailsShouldAggregateActiveContractsAndPayments() {
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        Client client = Client.builder()
                .id(clientId)
                .branchId(UUID.randomUUID())
                .firstName("Roman")
                .lastName("Romanov")
                .status(ClientStatus.ACTIVE)
                .build();
        Contract first = contract(clientId, BigDecimal.valueOf(80000));
        Contract second = contract(clientId, BigDecimal.valueOf(40000));
        LocalDateTime lastPaidAt = LocalDateTime.of(2026, 7, 20, 12, 0);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(contractRepository.findByClientId(clientId)).thenReturn(List.of(first, second));
        when(paymentPort.getContractPaymentSummaries(any())).thenReturn(Map.of(
                first.getId(), payment(first, BigDecimal.valueOf(60000), BigDecimal.valueOf(20000), LocalDateTime.of(2026, 7, 10, 12, 0)),
                second.getId(), payment(second, BigDecimal.valueOf(40000), BigDecimal.ZERO, lastPaidAt)
        ));
        when(relationPort.getClientStudents(clientId)).thenReturn(List.of(new ClientStudentRelationOutput(
                UUID.randomUUID(), clientId, "Roman Romanov", playerId, "Student One",
                ClientStudentRelationshipType.MOTHER, true, true, true, true,
                LocalDate.now(), null, true
        )));

        var output = service.getClient(clientId);

        assertEquals(1, output.summary().studentsCount());
        assertEquals(2, output.summary().activeContractsCount());
        assertEquals(BigDecimal.valueOf(100000), output.summary().money().paidAmount());
        assertEquals(BigDecimal.valueOf(20000), output.summary().money().outstandingAmount());
        assertEquals("KZT", output.summary().money().currency());
        assertFalse(output.summary().money().mixedCurrencies());
        assertEquals(lastPaidAt, output.summary().money().lastPaidAt());
        assertFalse(output.capabilities().canActivate());
        assertTrue(output.capabilities().canPause());
        assertTrue(output.capabilities().canDeactivate());
    }

    @Test
    void listShouldUseLivePaymentsThenFilterSortAndPaginate() {
        UUID branchId = UUID.randomUUID();
        Client paidClient = Client.builder().id(UUID.randomUUID()).branchId(branchId)
                .firstName("Paid").lastName("Client").status(ClientStatus.ACTIVE).build();
        Client debtor = Client.builder().id(UUID.randomUUID()).branchId(branchId)
                .firstName("Debt").lastName("Client").status(ClientStatus.ACTIVE).build();
        Contract paidContract = contract(paidClient.getId(), BigDecimal.valueOf(50000));
        Contract unpaidContract = contract(debtor.getId(), BigDecimal.valueOf(30000));
        LocalDateTime paidAt = LocalDateTime.of(2026, 7, 21, 9, 50);

        when(clientRepository.search(org.mockito.ArgumentMatchers.eq(branchId), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(paidClient, debtor)));
        when(relationRepository.findByClientIdInAndEndedAtIsNull(any())).thenReturn(List.of());
        when(contractRepository.findByClientIdIn(any())).thenReturn(List.of(paidContract, unpaidContract));
        when(paymentPort.getContractPaymentSummaries(any())).thenReturn(Map.of(
                paidContract.getId(), payment(paidContract, BigDecimal.valueOf(50000), BigDecimal.ZERO, paidAt),
                unpaidContract.getId(), new ContractPaymentSummaryOutput(
                        unpaidContract.getId(), unpaidContract.getAmount(), BigDecimal.ZERO,
                        BigDecimal.valueOf(30000), BigDecimal.ZERO, ContractPaymentStatus.UNPAID, null, 0
                )
        ));

        var page = service.getClients(
                branchId,
                new ClientWorkspaceListQuery(null, Set.of(), "ALL", "ALL", "ALL"),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "outstandingAmount"))
        );

        assertEquals(2, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(debtor.getId(), page.content().getFirst().id());
        assertEquals("UNPAID", page.content().getFirst().paymentStatus());
        assertEquals(BigDecimal.valueOf(30000), page.summary().outstandingAmount());
        assertEquals(BigDecimal.valueOf(50000), page.summary().paidAmount());

        var paidOnly = service.getClients(
                branchId,
                new ClientWorkspaceListQuery(null, Set.of("ACTIVE"), "ALL", "ACTIVE", "PAID"),
                PageRequest.of(0, 20, Sort.by("fullName"))
        );
        assertEquals(1, paidOnly.totalElements());
        assertEquals(paidClient.getId(), paidOnly.content().getFirst().id());
        assertEquals("PAID", paidOnly.content().getFirst().paymentStatus());
        assertEquals(BigDecimal.ZERO, paidOnly.content().getFirst().outstandingAmount());
        assertEquals(paidAt, paidOnly.content().getFirst().lastPaidAt());
    }

    @Test
    void createShouldPersistClientWithoutApplicationUser() {
        UUID branchId = UUID.randomUUID();
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientRepository.findById(any())).thenAnswer(invocation -> {
            UUID clientId = invocation.getArgument(0);
            return Optional.of(Client.builder()
                    .id(clientId)
                    .branchId(branchId)
                    .firstName("Roman")
                    .lastName("Romanov")
                    .phone("+77001234567")
                    .email("roman@example.com")
                    .status(ClientStatus.NEW)
                    .build());
        });
        when(contractRepository.findByClientId(any())).thenReturn(List.of());
        when(relationPort.getClientStudents(any())).thenReturn(List.of());

        var output = service.create(new ClientWorkspaceCreateCommand(
                branchId, " Roman ", " Romanov ", "+77001234567", "ROMAN@EXAMPLE.COM", "Referral", null
        ));

        assertEquals(ClientStatus.NEW.name(), output.client().status());
        assertEquals("roman@example.com", output.client().email());
        assertNotNull(output.client().id());
        verify(clientRepository).save(org.mockito.ArgumentMatchers.argThat(client ->
                client.getId() != null
                        && client.getUserId() == null
                        && "Roman".equals(client.getFirstName())
                        && "roman@example.com".equals(client.getEmail())
        ));
    }

    @Test
    void updateAndStatusShouldMutateExistingClient() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder()
                .id(clientId)
                .branchId(UUID.randomUUID())
                .firstName("Old")
                .status(ClientStatus.NEW)
                .build();
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(contractRepository.findByClientId(clientId)).thenReturn(List.of());
        when(relationPort.getClientStudents(clientId)).thenReturn(List.of());

        service.update(new ClientWorkspaceUpdateCommand(
                clientId, "New", "Name", null, "NEW@EXAMPLE.COM", "Web", "Updated"
        ));
        var output = service.changeStatus(new ClientWorkspaceStatusCommand(clientId, "active"));

        assertEquals("New", client.getFirstName());
        assertEquals("new@example.com", client.getEmail());
        assertEquals(ClientStatus.ACTIVE, client.getStatus());
        assertEquals(ClientStatus.ACTIVE.name(), output.client().status());
    }

    @Test
    void pauseShouldOnlyBeAllowedForActiveClient() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder()
                .id(clientId)
                .branchId(UUID.randomUUID())
                .firstName("New")
                .status(ClientStatus.NEW)
                .build();
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        ConflictException exception = assertThrows(ConflictException.class, () ->
                service.changeStatus(new ClientWorkspaceStatusCommand(clientId, "PAUSED"))
        );

        assertEquals("CLIENT_STATUS_TRANSITION_NOT_ALLOWED", exception.getErrorCode());
        assertEquals(ClientStatus.NEW, client.getStatus());
    }

    private Contract contract(UUID clientId, BigDecimal amount) {
        return Contract.builder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .playerId(UUID.randomUUID())
                .groupId(UUID.randomUUID())
                .status(ContractStatus.ACTIVE)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusMonths(1))
                .amount(amount)
                .currency("KZT")
                .build();
    }

    private ContractPaymentSummaryOutput payment(
            Contract contract,
            BigDecimal paid,
            BigDecimal outstanding,
            LocalDateTime lastPaidAt
    ) {
        return new ContractPaymentSummaryOutput(
                contract.getId(), contract.getAmount(), paid, outstanding, BigDecimal.ZERO,
                outstanding.signum() > 0 ? ContractPaymentStatus.PARTIALLY_PAID : ContractPaymentStatus.PAID,
                lastPaidAt, 1
        );
    }
}
