package kz.edu.soccerhub.trial.application.service;

import kz.edu.soccerhub.common.dto.trial.TrialSessionParticipantDto;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;

import java.util.List;

public interface TrialSessionParticipantReader {

    List<TrialSessionParticipantDto> read(
            List<TrialBooking> bookings
    );
}