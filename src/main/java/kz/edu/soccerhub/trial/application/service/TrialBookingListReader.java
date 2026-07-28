package kz.edu.soccerhub.trial.application.service;

import kz.edu.soccerhub.common.dto.trial.TrialBookingListItemDto;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import org.springframework.data.domain.Page;

public interface TrialBookingListReader {

    Page<TrialBookingListItemDto> read(Page<TrialBooking> bookings);
}