package com.blanchebridal.backend.appointment.service.impl;

import com.blanchebridal.backend.appointment.entity.Appointment;
import com.blanchebridal.backend.appointment.service.GoogleCalendarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GoogleCalendarServiceImpl implements GoogleCalendarService {

    @Override
    public String createEvent(Appointment appointment) {
        log.info("[GoogleCalendar] STUB — would create event for appointment {}",
                appointment.getId());
        return "stub-event-id-" + appointment.getId();
    }

    @Override
    public void updateEvent(String googleEventId, Appointment appointment) {
        log.info("[GoogleCalendar] STUB — would update event {} for appointment {}",
                googleEventId, appointment.getId());
    }

    @Override
    public void deleteEvent(String googleEventId) {
        log.info("[GoogleCalendar] STUB — would delete event {}", googleEventId);
    }
}