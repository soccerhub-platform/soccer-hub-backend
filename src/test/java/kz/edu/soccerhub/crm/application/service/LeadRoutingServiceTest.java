package kz.edu.soccerhub.crm.application.service;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.common.dto.lead.*;
import kz.edu.soccerhub.common.dto.trial.TrialBookingListItemDto;
import kz.edu.soccerhub.common.exception.*;
import kz.edu.soccerhub.crm.domain.model.*;
import kz.edu.soccerhub.crm.domain.model.enums.*;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class LeadRoutingServiceTest {
 final LeadRepository repository=mock(LeadRepository.class);final LeadActivityService history=mock(LeadActivityService.class);final TrialPort trials=mock(TrialPort.class);final LeadRoutingService service=new LeadRoutingService(repository,history,trials,mock(BranchPort.class),mock(AdminPort.class));
 final UUID id=UUID.randomUUID(),branch=UUID.randomUUID(),target=UUID.randomUUID(),actor=UUID.randomUUID();
 Lead lead(LeadStatus s) {var l=Lead.builder().id(id).branchId(branch).status(s).workVersion(5).build();when(repository.findLockedById(id)).thenReturn(Optional.of(l));return l;}
 LeadRoutingCommand command(long version) {return new LeadRoutingCommand(target,null,version,"Смена филиала");}
 @Test void staleRoutingDoesNotMutate() {var l=lead(LeadStatus.NEW);assertThrows(ConflictException.class,()->service.route(id,actor,command(4)));assertEquals(branch,l.getBranchId());verifyNoInteractions(history,trials);}
 @Test void closedLeadCannotMove() {lead(LeadStatus.CONVERTED);assertThrows(BadRequestException.class,()->service.route(id,actor,command(5)));verifyNoInteractions(history,trials);}
 @Test void linkedTrialPreventsCrossBranchTransfer() {var l=lead(LeadStatus.IN_PROGRESS);when(trials.findByLeadIds(List.of(id))).thenReturn(List.of(TrialBookingListItemDto.builder().id(UUID.randomUUID()).build()));assertThrows(BadRequestException.class,()->service.route(id,actor,command(5)));assertEquals(branch,l.getBranchId());verifyNoInteractions(history);}
 @Test void routingKeepsPlanAndIncrementsVersion() {var l=lead(LeadStatus.NEW);l.updateWork("HIGH","Позвонить",java.time.Instant.now().plusSeconds(3600),false);service.route(id,actor,command(6));assertEquals(target,l.getBranchId());assertEquals(7,l.getWorkVersion());assertEquals("Позвонить",l.getNextAction());verify(history).logWork(eq(l),eq(actor),contains("Смена филиала"));}
}
