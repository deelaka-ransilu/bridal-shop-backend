package com.blanchebridal.backend.appointment.dto.req;

import com.blanchebridal.backend.appointment.entity.AppointmentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateAppointmentRequest {

    private UUID productId; // optional

    @NotNull(message = "Appointment date is required")
    private LocalDate appointmentDate;

    @NotNull(message = "Time slot is required")
    private String timeSlot;

    @NotNull(message = "Appointment type is required")
    private AppointmentType type;

    private String notes;
}