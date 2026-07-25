package kz.edu.soccerhub.trial.application.service;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;

public interface TrialBookingDetailsReader {

    TrialBookingDetailsDto read(TrialBooking booking);
}