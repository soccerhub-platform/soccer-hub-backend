package kz.edu.soccerhub.trial.application.service;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.dto.trial.*;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import kz.edu.soccerhub.trial.domain.enums.*;
import kz.edu.soccerhub.trial.domain.repository.TrialBookingRepository;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.liquibase.enabled=false"})
class CompanyTrialReaderTest {
 @Autowired TrialBookingRepository repository;
 @Test void scopedSessionAndAttendanceFiltersApplyBeforePaginationAndSummary() {
  UUID own=UUID.randomUUID(),foreign=UUID.randomUUID();var a=TrialBooking.schedule(null,null,null,own);var b=TrialBooking.schedule(null,null,null,foreign);var c=TrialBooking.schedule(null,null,null,own);c.markAttendance(TrialAttendanceStatus.NO_SHOW,UUID.randomUUID(),"Не явился");repository.saveAllAndFlush(List.of(a,b,c));
  var list=mock(TrialBookingListReader.class);when(list.read(any())).thenAnswer(i->{org.springframework.data.domain.Page<TrialBooking> page=i.getArgument(0);return page.map(t->TrialBookingListItemDto.builder().id(t.getId()).trainingSessionId(t.getTrainingSessionId()).build());});
  var reader=new CompanyTrialReader(repository,list,mock(TrialSessionPort.class),mock(GroupPort.class),mock(LeadPort.class),mock(CompanyLeadOperationsPort.class));var today=LocalDate.now();var q=new CompanyQuery(Set.of(),"","SCHEDULED",today,today,0,25,true,"");var result=reader.page(Set.of(own),q);
  assertEquals(1,result.totalElements());assertEquals(a.getId(),result.content().getFirst().id());assertEquals(1,result.summary().get("NO_SHOW"));assertEquals(1,result.summary().get("SCHEDULED"));assertEquals(0,reader.page(Set.of(),q).totalElements());
 }
}
