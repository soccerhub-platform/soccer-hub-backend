package kz.edu.soccerhub.trial.application.service;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.dto.trial.*;
import kz.edu.soccerhub.common.port.TrialWorkflowPort;
import kz.edu.soccerhub.common.port.TrialPort;
import kz.edu.soccerhub.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@lombok.extern.slf4j.Slf4j
@Service @RequiredArgsConstructor
public class TrialWorkflowService implements TrialWorkflowPort {
 private final TrialPort trials;
 private final TrialOperationJournal journal;
 private final kz.edu.soccerhub.common.port.LeadPort leads;
 @Override @Transactional public void update(UUID id, UUID actor, TrialMutationCommand c) {
  if (c.operation() == TrialMutationCommand.Operation.ATTENDANCE) {
   var updated=trials.markAttendance(MarkTrialAttendanceCommand.builder().trialId(id).adminId(actor).version(c.version()).status(c.attendance()).comment(c.comment()).build());
   if(updated!=null&&updated.lead()!=null)syncLead(updated.lead().id(),actor,c.attendance());
  } else {
   if (c.result() == kz.edu.soccerhub.trial.domain.enums.TrialResult.CONVERTED) throw new BadRequestException("Оформление выполняется в карточке лида");
   trials.recordResult(RecordTrialResultCommand.builder().trialId(id).adminId(actor).version(c.version()).result(c.result()).coachFeedback(c.comment()).nextActionType(c.nextActionType()).nextActionAt(c.nextActionAt()).build());
  }
 }
 private void syncLead(UUID leadId,UUID actor,kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus attendance) {
  Runnable sync=()->{try {leads.processEvent(leadId,attendance==kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus.ATTENDED?kz.edu.soccerhub.crm.application.state.LeadEvent.COMPLETE_TRIAL:kz.edu.soccerhub.crm.application.state.LeadEvent.NO_SHOW,null,null,actor);}catch(BadRequestException e){log.warn("Lead attendance sync skipped: {}",leadId);}};
  if(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){@Override public void afterCommit(){sync.run();}});
  else sync.run();
 }
 @Override public List<TimelineEvent> history(UUID id) { return journal.history(id); }
}
