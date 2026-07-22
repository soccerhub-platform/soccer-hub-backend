package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.ClientStudentRelation;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ClientStudentRelationRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationEndCommand;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationOutput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationUpdateCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientStudentRelationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientStudentRelationService implements ClientStudentRelationPort {

    private final ClientStudentRelationRepository relationRepository;
    private final ClientRepository clientRepository;
    private final PlayerRepository playerRepository;

    @Override
    @Transactional(readOnly = true)
    public UUID getClientBranchId(UUID clientId) {
        return findClient(clientId).getBranchId();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getStudentBranchId(UUID playerId) {
        return playerBranchId(findPlayer(playerId));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getRelationBranchId(UUID relationId) {
        ClientStudentRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new NotFoundException("Client-student relation not found", relationId));
        return findClient(relation.getClientId()).getBranchId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientStudentRelationOutput> getClientStudents(UUID clientId) {
        findClient(clientId);
        return relationRepository.findByClientIdOrderByStartedAtDesc(clientId).stream()
                .map(this::toOutput)
                .sorted(relationOrder())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientStudentRelationOutput> getStudentClients(UUID playerId) {
        findPlayer(playerId);
        return relationRepository.findByPlayerIdOrderByStartedAtDesc(playerId).stream()
                .map(this::toOutput)
                .sorted(relationOrder())
                .toList();
    }

    @Override
    @Transactional
    public ClientStudentRelationOutput create(ClientStudentRelationCreateCommand command) {
        Client client = findClient(command.clientId());
        Player player = findPlayer(command.playerId());
        verifySameBranch(client, player);
        validateRelationshipType(command.relationshipType());
        validateSelf(command.relationshipType(), command.primaryContact(), command.primaryPayer(), command.legalRepresentative());
        if (command.startedAt().isAfter(LocalDate.now())) {
            throw new BadRequestException("Relation start date cannot be in the future", command.startedAt());
        }
        if (relationRepository.existsByClientIdAndPlayerIdAndEndedAtIsNull(client.getId(), player.getId())) {
            throw conflict("Active client-student relation already exists", "CLIENT_STUDENT_RELATION_EXISTS", client.getId(), player.getId());
        }

        List<ClientStudentRelation> activeRelations = relationRepository.findByPlayerIdAndEndedAtIsNull(player.getId());
        if (activeRelations.isEmpty() && (!command.primaryContact() || !command.primaryPayer())) {
            throw new BadRequestException("First active relation must be primary contact and primary payer", player.getId());
        }
        validateSingleSelf(null, command.relationshipType(), activeRelations);
        requireExplicitPrimaryTransfer(
                activeRelations,
                command.primaryContact(),
                command.primaryPayer(),
                command.replacePrimaryContact(),
                command.replacePrimaryPayer()
        );
        transferPrimaryRoles(activeRelations, null, command.primaryContact(), command.primaryPayer());
        flushTransferredPrimaryRoles(activeRelations, command.primaryContact(), command.primaryPayer());

        ClientStudentRelation relation = relationRepository.save(ClientStudentRelation.builder()
                .clientId(client.getId())
                .playerId(player.getId())
                .relationshipType(command.relationshipType())
                .primaryContact(command.primaryContact())
                .primaryPayer(command.primaryPayer())
                .legalRepresentative(command.legalRepresentative())
                .receivesNotifications(command.receivesNotifications())
                .startedAt(command.startedAt())
                .build());
        syncLegacyParent(player, client, relation.isPrimaryContact());
        return toOutput(relation, client, player);
    }

    @Override
    @Transactional
    public ClientStudentRelationOutput createStudent(ClientStudentCreateCommand command) {
        Client client = findClient(command.clientId());
        String firstName = command.firstName() == null ? "" : command.firstName().trim();
        String lastName = command.lastName() == null ? "" : command.lastName().trim();
        if (firstName.isEmpty()) {
            throw new BadRequestException("Student first name is required", command.clientId());
        }
        if (command.birthDate() == null || !command.birthDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Student birth date must be in the past", command.birthDate());
        }
        if (playerRepository.findFirstByParent_IdAndFirstNameAndLastNameAndBirthDate(
                client.getId(), firstName, lastName, command.birthDate()).isPresent()) {
            throw conflict("Student with the same data already exists", "CLIENT_STUDENT_DUPLICATE", client.getId(), null);
        }

        Player player = playerRepository.save(Player.builder()
                .id(UUID.randomUUID())
                .firstName(firstName)
                .lastName(lastName)
                .birthDate(command.birthDate())
                .parent(client)
                .build());
        return create(new ClientStudentRelationCreateCommand(
                client.getId(), player.getId(), command.relationshipType(), command.primaryContact(),
                command.primaryPayer(), false, false, command.legalRepresentative(), command.receivesNotifications(),
                command.startedAt()
        ));
    }

    @Override
    @Transactional
    public ClientStudentRelationOutput update(ClientStudentRelationUpdateCommand command) {
        ClientStudentRelation relation = findLockedRelation(command.relationId());
        ensureActive(relation);
        Client client = findClient(relation.getClientId());
        Player player = findPlayer(relation.getPlayerId());
        verifySameBranch(client, player);
        validateRelationshipType(command.relationshipType());
        validateSelf(command.relationshipType(), command.primaryContact(), command.primaryPayer(), command.legalRepresentative());

        List<ClientStudentRelation> activeRelations = relationRepository.findByPlayerIdAndEndedAtIsNull(player.getId());
        validateSingleSelf(relation.getId(), command.relationshipType(), activeRelations);
        validatePrimaryRoleRemoval(relation, activeRelations, command.primaryContact(), command.primaryPayer());
        transferPrimaryRoles(activeRelations, relation.getId(), command.primaryContact(), command.primaryPayer());
        flushTransferredPrimaryRoles(activeRelations, command.primaryContact(), command.primaryPayer());

        relation.setRelationshipType(command.relationshipType());
        relation.setPrimaryContact(command.primaryContact());
        relation.setPrimaryPayer(command.primaryPayer());
        relation.setLegalRepresentative(command.legalRepresentative());
        relation.setReceivesNotifications(command.receivesNotifications());
        syncLegacyParent(player, client, relation.isPrimaryContact());
        return toOutput(relation, client, player);
    }

    @Override
    @Transactional
    public ClientStudentRelationOutput end(ClientStudentRelationEndCommand command) {
        ClientStudentRelation relation = findLockedRelation(command.relationId());
        ensureActive(relation);
        Client client = findClient(relation.getClientId());
        Player player = findPlayer(relation.getPlayerId());
        verifySameBranch(client, player);

        if (command.endedAt().isBefore(relation.getStartedAt())) {
            throw new BadRequestException("Relation end date cannot be before start date", command.endedAt());
        }
        if (command.endedAt().isAfter(LocalDate.now())) {
            throw new BadRequestException("Relation end date cannot be in the future", command.endedAt());
        }
        List<ClientStudentRelation> activeRelations = relationRepository.findByPlayerIdAndEndedAtIsNull(player.getId());
        if (activeRelations.size() <= 1) {
            throw conflict("Cannot end the last active student relation", "CLIENT_STUDENT_LAST_RELATION", relation.getId(), player.getId());
        }
        if (relation.isPrimaryContact() || relation.isPrimaryPayer()) {
            throw conflict("Transfer primary roles before ending relation", "CLIENT_STUDENT_PRIMARY_TRANSFER_REQUIRED", relation.getId(), player.getId());
        }

        relation.setEndedAt(command.endedAt());
        return toOutput(relation, client, player);
    }

    private void validateRelationshipType(ClientStudentRelationshipType type) {
        if (type == ClientStudentRelationshipType.LEGACY_PARENT) {
            throw new BadRequestException("LEGACY_PARENT is reserved for migrated data", type);
        }
    }

    private void validateSelf(ClientStudentRelationshipType type, boolean contact, boolean payer, boolean legal) {
        if (type == ClientStudentRelationshipType.SELF && (!contact || !payer || !legal)) {
            throw new BadRequestException("SELF relation must be primary contact, payer and legal representative", type);
        }
    }

    private void validateSingleSelf(UUID currentId, ClientStudentRelationshipType type, List<ClientStudentRelation> active) {
        if (type == ClientStudentRelationshipType.SELF && active.stream().anyMatch(item ->
                !Objects.equals(item.getId(), currentId) && item.getRelationshipType() == ClientStudentRelationshipType.SELF)) {
            throw conflict("Student already has an active SELF relation", "CLIENT_STUDENT_SELF_EXISTS", currentId, null);
        }
    }

    private void validatePrimaryRoleRemoval(ClientStudentRelation relation, List<ClientStudentRelation> active, boolean contact, boolean payer) {
        if (relation.isPrimaryContact() && !contact
                && active.stream().noneMatch(item -> !item.getId().equals(relation.getId()) && item.isPrimaryContact())) {
            throw conflict("Assign another primary contact first", "CLIENT_STUDENT_PRIMARY_CONTACT_REQUIRED", relation.getId(), relation.getPlayerId());
        }
        if (relation.isPrimaryPayer() && !payer
                && active.stream().noneMatch(item -> !item.getId().equals(relation.getId()) && item.isPrimaryPayer())) {
            throw conflict("Assign another primary payer first", "CLIENT_STUDENT_PRIMARY_PAYER_REQUIRED", relation.getId(), relation.getPlayerId());
        }
    }

    private void transferPrimaryRoles(List<ClientStudentRelation> active, UUID currentId, boolean contact, boolean payer) {
        active.stream().filter(item -> !Objects.equals(item.getId(), currentId)).forEach(item -> {
            if (contact) item.setPrimaryContact(false);
            if (payer) item.setPrimaryPayer(false);
        });
    }

    private void requireExplicitPrimaryTransfer(
            List<ClientStudentRelation> active,
            boolean primaryContact,
            boolean primaryPayer,
            boolean replacePrimaryContact,
            boolean replacePrimaryPayer
    ) {
        if (primaryContact && !replacePrimaryContact && active.stream().anyMatch(ClientStudentRelation::isPrimaryContact)) {
            throw conflict(
                    "Confirm replacement of the current primary contact",
                    "CLIENT_STUDENT_PRIMARY_CONTACT_REPLACEMENT_REQUIRED",
                    null,
                    active.getFirst().getPlayerId()
            );
        }
        if (primaryPayer && !replacePrimaryPayer && active.stream().anyMatch(ClientStudentRelation::isPrimaryPayer)) {
            throw conflict(
                    "Confirm replacement of the current primary payer",
                    "CLIENT_STUDENT_PRIMARY_PAYER_REPLACEMENT_REQUIRED",
                    null,
                    active.getFirst().getPlayerId()
            );
        }
    }

    private void flushTransferredPrimaryRoles(List<ClientStudentRelation> active, boolean contact, boolean payer) {
        if (!active.isEmpty() && (contact || payer)) relationRepository.flush();
    }

    private void syncLegacyParent(Player player, Client client, boolean primaryContact) {
        if (primaryContact && (player.getParent() == null || !Objects.equals(player.getParent().getId(), client.getId()))) {
            player.setParent(client);
            playerRepository.save(player);
        }
    }

    private ClientStudentRelationOutput toOutput(ClientStudentRelation relation) {
        return toOutput(relation, findClient(relation.getClientId()), findPlayer(relation.getPlayerId()));
    }

    private ClientStudentRelationOutput toOutput(ClientStudentRelation relation, Client client, Player player) {
        return new ClientStudentRelationOutput(
                relation.getId(), client.getId(), fullName(client.getFirstName(), client.getLastName()),
                player.getId(), fullName(player.getFirstName(), player.getLastName()), relation.getRelationshipType(),
                relation.isPrimaryContact(), relation.isPrimaryPayer(), relation.isLegalRepresentative(),
                relation.isReceivesNotifications(), relation.getStartedAt(), relation.getEndedAt(), relation.getEndedAt() == null
        );
    }

    private Comparator<ClientStudentRelationOutput> relationOrder() {
        return Comparator.comparing(ClientStudentRelationOutput::active).reversed()
                .thenComparing(ClientStudentRelationOutput::startedAt, Comparator.reverseOrder());
    }

    private Client findClient(UUID id) {
        return clientRepository.findById(id).orElseThrow(() -> new NotFoundException("Client not found", id));
    }

    private Player findPlayer(UUID id) {
        return playerRepository.findWithParentById(id).orElseThrow(() -> new NotFoundException("Player not found", id));
    }

    private ClientStudentRelation findLockedRelation(UUID id) {
        return relationRepository.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("Client-student relation not found", id));
    }

    private void ensureActive(ClientStudentRelation relation) {
        if (relation.getEndedAt() != null) {
            throw conflict("Client-student relation is already ended", "CLIENT_STUDENT_RELATION_ENDED", relation.getId(), relation.getPlayerId());
        }
    }

    private void verifySameBranch(Client client, Player player) {
        if (!Objects.equals(client.getBranchId(), playerBranchId(player))) {
            throw new BadRequestException("Client and student belong to different branches", client.getId(), player.getId());
        }
    }

    private UUID playerBranchId(Player player) {
        return player.getParent() == null ? null : player.getParent().getBranchId();
    }

    private ConflictException conflict(String message, String code, UUID relationOrClientId, UUID playerId) {
        return new ConflictException(message, code, Map.of(
                "relationOrClientId", String.valueOf(relationOrClientId), "playerId", String.valueOf(playerId)));
    }

    private String fullName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim())).trim();
    }
}
