package kz.edu.soccerhub.trial.application.service;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.dto.trial.*;
import kz.edu.soccerhub.common.port.TrialPort;
import kz.edu.soccerhub.common.exception.*;
import kz.edu.soccerhub.trial.domain.enums.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class TrialWorkflowServiceTest {
 final TrialPort trials=mock(TrialPort.class); final TrialOperationJournal journal=mock(TrialOperationJournal.class);
 final kz.edu.soccerhub.common.port.LeadPort leads=mock(kz.edu.soccerhub.common.port.LeadPort.class);
 final TrialWorkflowService service=new TrialWorkflowService(trials,journal,leads);
 final UUID id=UUID.randomUUID(),actor=UUID.randomUUID();
 @Test void sharedConflictIsNotSwallowed() {
  when(trials.markAttendance(any())).thenThrow(new ConflictException("stale","TRIAL_WORK_CONFLICT",Map.of()));
  assertThrows(ConflictException.class,()->service.update(id,actor,new TrialMutationCommand(TrialMutationCommand.Operation.ATTENDANCE,1L,TrialAttendanceStatus.ATTENDED,null,null,null,null)));
  verifyNoInteractions(journal);
 }
 @Test void outcomeIsSentThroughOwningService() {
  service.update(id,actor,new TrialMutationCommand(TrialMutationCommand.Operation.RESULT,2L,null,TrialResult.INTERESTED,null,null,"Комментарий"));
  verify(trials).recordResult(argThat(c->c.trialId().equals(id)&&c.adminId().equals(actor)&&c.version()==2&&c.result()==TrialResult.INTERESTED));
 }
 @Test void attendanceKeepsActorVersionAndComment() {
  service.update(id,actor,new TrialMutationCommand(TrialMutationCommand.Operation.ATTENDANCE,3L,TrialAttendanceStatus.NO_SHOW,null,null,null,"Не пришёл"));
  verify(trials).markAttendance(argThat(c->c.adminId().equals(actor)&&c.version()==3&&c.status()==TrialAttendanceStatus.NO_SHOW&&c.comment().equals("Не пришёл")));
 }
 @Test void dispatcherAttendanceSyncsTheSameLeadWithItsActor() {
  var leadId=UUID.randomUUID();when(trials.markAttendance(any())).thenReturn(TrialBookingDetailsDto.builder().lead(TrialBookingDetailsDto.Lead.builder().id(leadId).build()).build());
  service.update(id,actor,new TrialMutationCommand(TrialMutationCommand.Operation.ATTENDANCE,3L,TrialAttendanceStatus.ATTENDED,null,null,null,null));
  verify(leads).processEvent(leadId,kz.edu.soccerhub.crm.application.state.LeadEvent.COMPLETE_TRIAL,null,null,actor);
 }
 @Test void dispatcherCannotFakeConversion() {assertThrows(BadRequestException.class,()->service.update(id,actor,new TrialMutationCommand(TrialMutationCommand.Operation.RESULT,0L,null,TrialResult.CONVERTED,null,null,null)));verifyNoInteractions(trials);}
}
