package kz.edu.soccerhub.dispatcher.application.service;

import kz.edu.soccerhub.common.dto.club.ClubDto;
import kz.edu.soccerhub.common.port.ClubPort;
import kz.edu.soccerhub.dispatcher.domain.model.DispatcherClub;
import kz.edu.soccerhub.dispatcher.domain.repository.DispatcherClubRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DispatcherClubServiceTest {
    @Test
    void returnsClubEmailAlongsideOtherContactDetails() {
        var repository = mock(DispatcherClubRepository.class);
        var clubPort = mock(ClubPort.class);
        var dispatcherId = UUID.randomUUID();
        var clubId = UUID.randomUUID();
        var assignment = new DispatcherClub.DispatcherClubId();
        assignment.setDispatcherId(dispatcherId);
        assignment.setClubId(clubId);
        when(repository.findByIdDispatcherId(dispatcherId)).thenReturn(List.of(DispatcherClub.builder().id(assignment).build()));
        when(clubPort.findAllByIds(List.of(clubId))).thenReturn(List.of(ClubDto.builder()
                .id(clubId).name("QA club").email("club@qa.soccerhub.test").phoneNumber("+77000000001").build()));
        var output = new DispatcherClubService(repository, clubPort).getDispatcherClubs(dispatcherId).getFirst();
        assertEquals("club@qa.soccerhub.test", output.email());
        assertEquals("+77000000001", output.phoneNumber());
    }
}
