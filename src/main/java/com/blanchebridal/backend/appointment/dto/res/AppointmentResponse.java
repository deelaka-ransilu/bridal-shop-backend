package com.blanchebridal.backend.appointment.dto.res;

import com.blanchebridal.backend.appointment.entity.AppointmentStatus;
import com.blanchebridal.backend.appointment.entity.AppointmentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AppointmentResponse {

    private UUID id;
    private UUID userId;
    private String customerName;
    private String customerEmail;
    private UUID productId;
    private String productName;
    private LocalDate appointmentDate;
    private String timeSlot;
    private AppointmentType type;
    private AppointmentStatus status;
    private String googleEventId;
    private String notes;
    private LocalDateTime createdAt;
}