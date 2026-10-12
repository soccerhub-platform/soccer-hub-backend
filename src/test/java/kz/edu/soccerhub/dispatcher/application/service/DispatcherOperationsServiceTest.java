package kz.edu.soccerhub.dispatcher.application.service;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.trial.*;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.common.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class DispatcherOperationsServiceTest {
 @Mock DispatcherCompanyService company;@Mock DispatcherBranchService branches;@Mock DispatcherLeadWorkflowService leadWorkflow;
 @Mock CompanyLeadOperationsPort leads;@Mock ContractFollowUpPort followUp;@Mock ContractPort contracts;@Mock AdminPort admins;
 @Mock CompanyOrganizationPort organization;@Mock CompanySessionReadPort sessionReader;@Mock TrialSessionPort sessions;@Mock GroupPort groups;@Mock CoachPort coaches;@Mock TrialPort trials;@Mock TrialWorkflowPort trialWorkflow;@Mock CompanyTrialReadPort trialReader;@Mock CompanyStudentHistoryPort students;@Mock CompanyPaymentHistoryPort payments;@Mock LeadPort leadPort;
 @InjectMocks DispatcherOperationsService service;
 UUID user=UUID.randomUUID(),branch=UUID.randomUUID(),id=UUID.randomUUID();
 @Test void contractFollowUpRequiresContractBranchOwnership() {
  when(contracts.getBranchId(id)).thenReturn(branch);doThrow(new BadRequestException("Нет доступа")).when(branches).requireOwnedBranch(user,branch);
  assertThrows(BadRequestException.class,()->service.followUp(user,id));
  assertThrows(BadRequestException.class,()->service.updateFollowUp(user,id,FollowUpCommand.builder().operation(FollowUpCommand.Operation.PLAN).version(0L).build()));
  verifyNoInteractions(followUp,leadWorkflow);
 }
 @Test void trialScopeComesFromSessionGroupAndCannotBeBypassedByTrialId() {
  UUID session=UUID.randomUUID(),group=UUID.randomUUID();when(trials.getTrial(id)).thenReturn(TrialBookingDto.builder().id(id).trainingSessionId(session).build());
  when(sessions.getSessionDetails(session)).thenReturn(TrialSessionContext.builder().sessionId(session).groupId(group).build());when(groups.getGroupById(group)).thenReturn(GroupDto.builder().groupId(group).branchId(branch).build());
  doThrow(new BadRequestException("Нет доступа")).when(branches).requireOwnedBranch(user,branch);
  assertThrows(BadRequestException.class,()->service.trial(user,id));assertThrows(BadRequestException.class,()->service.updateTrial(user,id,TrialMutationCommand.builder().operation(TrialMutationCommand.Operation.ATTENDANCE).version(0L).build()));verifyNoInteractions(trialWorkflow);verify(trials,never()).getTrialDetails(any());
 }
 @Test void unifiedQueueMergesByDeadlineAndPriorityWithExactTotals() {
  LocalDate today=LocalDate.now(ZoneId.of("Asia/Almaty"));CompanyQuery q=new CompanyQuery(Set.of(branch),"","",today,today,0,25,true,"");when(company.scopedQuery(eq(user),eq(branch),eq(""),eq(""),any(),any(),eq(0),eq(25))).thenReturn(q);
  var due=Instant.now().plusSeconds(3600);var l=OperationsRow.builder().id(UUID.randomUUID()).branchId(branch).kind("LEAD").dueAt(due).priority("NORMAL").build();var c=OperationsRow.builder().id(UUID.randomUUID()).branchId(branch).kind("CONTRACT").dueAt(due).priority("HIGH").build();var t=OperationsRow.builder().id(UUID.randomUUID()).branchId(branch).kind("TRIAL").dueAt(due.minusSeconds(1)).priority("NORMAL").build();
  when(leads.tasks(any())).thenReturn(new OperationsPage(List.of(l),5,1,0,25));when(followUp.tasks(any())).thenReturn(new OperationsPage(List.of(c),7,1,0,25));when(organization.groupBranches(Set.of(branch))).thenReturn(Map.of());when(sessionReader.sessions(Set.of(),null,null)).thenReturn(Map.of());when(trialReader.tasks(eq(Set.of()),any())).thenReturn(new OperationsPage(List.of(t),2,1,0,25));when(admins.findAllByDispatcherId(user)).thenReturn(List.of());
  var result=service.tasks(user,branch,"ALL","","",0,25);assertEquals(14,result.totalElements());assertEquals(List.of(t.id(),c.id(),l.id()),result.content().stream().map(OperationsRow::id).toList());assertEquals(7,result.summary().get("CONTRACT"));
 }
 @Test void invalidMixedQueueFiltersFailBeforeReads() {assertThrows(BadRequestException.class,()->service.tasks(user,branch,"ALL","NO_ACTION","",0,25));assertThrows(BadRequestException.class,()->service.tasks(user,branch,"TRIAL","","search",0,25));verifyNoInteractions(company,leads,followUp,trialReader);}
 @Test void studentHistoryNeverMixesSiblingTrialsFromTheSameLead() {
  UUID leadId=UUID.randomUUID(),participant=UUID.randomUUID(),siblingParticipant=UUID.randomUUID(),sibling=UUID.randomUUID(),trialId=UUID.randomUUID(),otherTrial=UUID.randomUUID(),session=UUID.randomUUID(),group=UUID.randomUUID();
  var today=LocalDate.now();var q=new CompanyQuery(Set.of(branch),"","",today,today,0,25,false,"");
  when(company.scopedQuery(user,null,"","",null,null,0,25)).thenReturn(q);when(students.student(id,Set.of(branch))).thenReturn(CompanyEntry.builder().id(id).branchId(branch).build());
  when(leads.studentLeads(id,Set.of(branch))).thenReturn(List.of(leadId));
  when(leadPort.getLeadOutput(leadId,user)).thenReturn(kz.edu.soccerhub.common.dto.lead.LeadOutput.builder().id(leadId).participants(List.of(kz.edu.soccerhub.common.dto.lead.LeadParticipantOutput.builder().id(participant).playerId(id).build(),kz.edu.soccerhub.common.dto.lead.LeadParticipantOutput.builder().id(siblingParticipant).playerId(sibling).build())).build());
  when(trials.findList(any(),any())).thenReturn(org.springframework.data.domain.Page.empty());
  var own=TrialBookingListItemDto.builder().id(trialId).participantId(participant).trainingSessionId(session).build();var other=TrialBookingListItemDto.builder().id(otherTrial).participantId(siblingParticipant).trainingSessionId(session).build();
  when(trials.findByLeadIds(List.of(leadId))).thenReturn(List.of(own,other));when(sessions.getSessionDetails(session)).thenReturn(TrialSessionContext.builder().groupId(group).build());when(groups.getGroupById(group)).thenReturn(GroupDto.builder().branchId(branch).build());
  when(trialWorkflow.history(trialId)).thenReturn(List.of(new TimelineEvent(UUID.randomUUID(),"ATTENDANCE","Посетил",Instant.now(),user)));
  var h=service.student(user,id);assertEquals(List.of(trialId),h.trials().stream().map(TrialBookingListItemDto::id).toList());assertEquals(1,h.events().size());verify(trialWorkflow,never()).history(otherTrial);
 }
}
