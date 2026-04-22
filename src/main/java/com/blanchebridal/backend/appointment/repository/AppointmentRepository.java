package com.blanchebridal.backend.appointment.repository;

import com.blanchebridal.backend.appointment.entity.Appointment;
import com.blanchebridal.backend.appointment.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findByUser_Id(UUID userId, Pageable pageable);

    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);

    // All non-cancelled appointments on a date — used to find booked slots
    List<Appointment> findByAppointmentDateAndStatusNot(
            LocalDate date, AppointmentStatus status);

    // Used to check if a specific slot is already taken on a date
    boolean existsByAppointmentDateAndTimeSlotAndStatusNot(
            LocalDate date, String timeSlot, AppointmentStatus status);
}