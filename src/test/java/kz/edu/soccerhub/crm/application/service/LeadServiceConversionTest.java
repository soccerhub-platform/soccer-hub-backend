package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.auth.domain.model.AppUserEntity;
import kz.edu.soccerhub.auth.domain.repository.AppRoleRepo;
import kz.edu.soccerhub.auth.domain.repository.AppUserRepo;
import kz.edu.soccerhub.client.domain.model.Client;
import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.ClientRepository;
import kz.edu.soccerhub.client.domain.repository.ContractRepository;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadRequest;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadResponse;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.crm.application.mapper.LeadMapper;
import kz.edu.soccerhub.crm.application.state.LeadStateMachineService;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadChild;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.repository.LeadLossReasonRepository;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import kz.edu.soccerhub.organization.domain.model.Group;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import kz.edu.soccerhub.organization.domain.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceConversionTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private LeadStateMachineService stateMachineService;
    @Mock
    private AdminPort adminPort;
    @Mock
    private GroupPort groupPort;
    @Mock
    private CoachPort coachPort;
    @Mock
    private GroupSchedulePort groupSchedulePort;
    @Mock
    private LeadActivityService leadActivityService;
    @Mock
    private LeadMapper leadMapper;
    @Mock
    private LeadLossReasonRepository leadLossReasonRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private AppUserRepo appUserRepo;
    @Mock
    private AppRoleRepo appRoleRepo;
    @Mock
    private PasswordEncoder passwordEncoder;

    private LeadService leadService;

    @BeforeEach
    void setUp() {
        leadService = new LeadService(
                leadRepository,
                stateMachineService,
                adminPort,
                groupPort,
                coachPort,
                groupSchedulePort,
                leadActivityService,
                leadMapper,
                leadLossReasonRepository,
                clientRepository,
                playerRepository,
                contractRepository,
                groupRepository,
                appUserRepo,
                appRoleRepo,
                passwordEncoder,
                new ObjectMapper()
        );
    }

    @Test
    void convertValidLeadCreatesClientPlayerContractAndUpdatesLead() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, branchId, childId, "Alex Doe");
        ConvertLeadRequest request = new ConvertLeadRequest(
                childId,
                groupId,
                LocalDate.of(2016, 5, 20),
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 6, 20),
                new BigDecimal("30000")
        );

        Group group = Group.builder().id(groupId).branchId(branchId).name("U10").status(GroupStatus.ACTIVE).build();

        AppUserEntity appUser = AppUserEntity.builder().id(clientId).email("parent@example.com").build();
        Client client = Client.builder().id(clientId).firstName("Parent").lastName("One").build();
        Player player = Player.builder().id(playerId).firstName("Alex").lastName("Doe").build();
        Contract contract = Contract.builder().id(contractId).playerId(playerId).groupId(groupId).build();

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(appUserRepo.findByEmail("parent@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(appUserRepo.save(any(AppUserEntity.class))).thenReturn(appUser);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(playerRepository.findFirstByParent_IdAndFirstNameAndLastNameAndBirthDate(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(playerRepository.save(any(Player.class))).thenReturn(player);
        when(contractRepository.findFirstByPlayerIdAndGroupIdAndStartDateAndEndDate(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(contractRepository.save(any(Contract.class))).thenReturn(contract);

        ConvertLeadResponse response = leadService.convertLeadToClient(leadId, request, actorId);

        assertEquals(leadId, response.leadId());
        assertEquals(clientId, response.clientId());
        assertEquals(playerId, response.playerId());
        assertEquals(contractId, response.contractId());
        assertEquals("CONVERTED", response.status());
        assertEquals(LeadStatus.WON, lead.getStatus());
        assertEquals(clientId, lead.getClientId());

        verify(leadRepository).save(lead);
        verify(leadActivityService).logLeadConverted(eq(lead), eq(actorId), eq(LeadStatus.TRIAL_DONE), contains("\"childId\""));
    }

    @Test
    void convertFromWaitingPaymentShouldBeAllowed() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.WAITING_PAYMENT, branchId, childId, "Alex Doe");
        ConvertLeadRequest request = new ConvertLeadRequest(
                childId,
                groupId,
                LocalDate.of(2016, 5, 20),
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 6, 20),
                new BigDecimal("30000")
        );

        Group group = Group.builder().id(groupId).branchId(branchId).name("U10").status(GroupStatus.ACTIVE).build();
        AppUserEntity appUser = AppUserEntity.builder().id(clientId).email("parent@example.com").build();
        Client client = Client.builder().id(clientId).firstName("Parent").lastName("One").build();
        Player player = Player.builder().id(playerId).firstName("Alex").lastName("Doe").build();
        Contract contract = Contract.builder().id(contractId).playerId(playerId).groupId(groupId).build();

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(appUserRepo.findByEmail("parent@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(appUserRepo.save(any(AppUserEntity.class))).thenReturn(appUser);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(playerRepository.findFirstByParent_IdAndFirstNameAndLastNameAndBirthDate(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(playerRepository.save(any(Player.class))).thenReturn(player);
        when(contractRepository.findFirstByPlayerIdAndGroupIdAndStartDateAndEndDate(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(contractRepository.save(any(Contract.class))).thenReturn(contract);

        ConvertLeadResponse response = leadService.convertLeadToClient(leadId, request, actorId);

        assertEquals("CONVERTED", response.status());
        assertEquals(LeadStatus.WON, lead.getStatus());
        verify(leadRepository).save(lead);
    }

    @Test
    void convertChildFromAnotherLeadShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID foreignChildId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, branchId, foreignChildId, "Foreign Kid");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        ConvertLeadRequest request = new ConvertLeadRequest(
                childId,
                UUID.randomUUID(),
                LocalDate.of(2016, 5, 20),
                LocalDate.of(2026, 5, 20),
                null,
                new BigDecimal("30000")
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> leadService.convertLeadToClient(leadId, request, actorId));
        assertTrue(ex.getMessage().contains("Child does not belong"));
    }

    @Test
    void convertWithGroupFromAnotherBranchShouldFail() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID leadBranch = UUID.randomUUID();
        UUID otherBranch = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, leadBranch, childId, "Alex Doe");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(Group.builder().id(groupId).branchId(otherBranch).name("U12").build()));

        ConvertLeadRequest request = new ConvertLeadRequest(
                childId,
                groupId,
                LocalDate.of(2016, 5, 20),
                LocalDate.of(2026, 5, 20),
                null,
                null
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> leadService.convertLeadToClient(leadId, request, actorId));
        assertTrue(ex.getMessage().contains("does not belong to lead branch"));
    }

    @Test
    void convertWithMissingGroupShouldReturnNotFound() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID leadBranch = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, leadBranch, childId, "Alex Doe");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        ConvertLeadRequest request = new ConvertLeadRequest(
                childId,
                groupId,
                LocalDate.of(2016, 5, 20),
                LocalDate.of(2026, 5, 20),
                null,
                null
        );

        assertThrows(NotFoundException.class, () -> leadService.convertLeadToClient(leadId, request, actorId));
    }

    @Test
    void convertAlreadyConvertedLeadShouldNotCreateDuplicates() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        Lead lead = lead(leadId, LeadStatus.WON, branchId, childId, "Alex Doe");
        lead.markConverted(clientId);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(Group.builder().id(groupId).branchId(branchId).name("U10").build()));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(Client.builder().id(clientId).build()));
        when(playerRepository.findFirstByParent_IdAndFirstNameAndLastNameAndBirthDate(any(), any(), any(), any()))
                .thenReturn(Optional.of(Player.builder().id(playerId).build()));
        when(contractRepository.findFirstByPlayerIdAndGroupIdAndStartDateAndEndDate(any(), any(), any(), any()))
                .thenReturn(Optional.of(Contract.builder().id(contractId).playerId(playerId).groupId(groupId).build()));

        ConvertLeadRequest request = new ConvertLeadRequest(
                childId,
                groupId,
                LocalDate.of(2016, 5, 20),
                LocalDate.of(2026, 5, 20),
                null,
                new BigDecimal("25000")
        );

        ConvertLeadResponse response = leadService.convertLeadToClient(leadId, request, actorId);

        assertEquals(clientId, response.clientId());
        assertEquals(playerId, response.playerId());
        assertEquals(contractId, response.contractId());
        verify(clientRepository, never()).save(any(Client.class));
        verify(playerRepository, never()).save(any(Player.class));
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    void convertWithoutChildBirthDateShouldFailValidation() {
        UUID leadId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.TRIAL_DONE, branchId, childId, "Alex Doe");
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

        ConvertLeadRequest request = new ConvertLeadRequest(
                childId,
                UUID.randomUUID(),
                null,
                LocalDate.of(2026, 5, 20),
                null,
                null
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> leadService.convertLeadToClient(leadId, request, actorId));
        assertTrue(ex.getMessage().contains("childBirthDate"));
    }

    private Lead lead(UUID leadId, LeadStatus status, UUID branchId, UUID childId, String childName) {
        Lead lead = Lead.builder()
                .id(leadId)
                .parentName("Parent One")
                .phone("+77001112233")
                .email("parent@example.com")
                .source(LeadSource.OTHER)
                .status(status)
                .branchId(branchId)
                .build();

        LeadChild child = LeadChild.builder()
                .id(childId)
                .lead(lead)
                .childName(childName)
                .childAge(10)
                .build();
        lead.getChildren().add(child);
        return lead;
    }
}
