package edu.bridalshop.backend.service;

import edu.bridalshop.backend.dto.request.DamageItemRequest;
import edu.bridalshop.backend.dto.request.RentalCreateRequest;
import edu.bridalshop.backend.dto.request.RentalReturnRequest;
import edu.bridalshop.backend.dto.response.RentalResponse;
import edu.bridalshop.backend.entity.*;
import edu.bridalshop.backend.exception.ResourceNotFoundException;
import edu.bridalshop.backend.repository.*;
import edu.bridalshop.backend.util.PayloadSanitizer;
import edu.bridalshop.backend.util.PublicIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock RentalRepository                    rentalRepository;
    @Mock RentalDamageItemRepository          damageItemRepository;
    @Mock DressRepository                     dressRepository;
    @Mock DressFulfillmentOptionRepository    fulfillmentRepository;
    @Mock UserRepository                      userRepository;
    @Mock PublicIdGenerator                   publicIdGenerator;
    @Mock PayloadSanitizer                    sanitizer;

    @InjectMocks RentalService service;

    @Captor ArgumentCaptor<Rental> rentalCaptor;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final BigDecimal PER_DAY = new BigDecimal("5000.00");
    private static final BigDecimal DEPOSIT  = new BigDecimal("10000.00");
    private static final int        DAYS     = 3;

    private User buildAdmin() {
        return User.builder()
                .userId(1).publicId("usr_admin").email("admin@test.com")
                .role(UserRole.ADMIN).isActive(true).build();
    }

    private User buildCustomer() {
        return User.builder()
                .userId(2).publicId("usr_cust1").email("cust@test.com")
                .role(UserRole.CUSTOMER).isActive(true).build();
    }

    private Category buildCategory() {
        return Category.builder()
                .categoryId(1).publicId("cat_test")
                .name("Ballgown").dressType("BRIDAL").isActive(true).build();
    }

    private Dress buildAvailableDress() {
        return Dress.builder()
                .dressId(1).publicId("drs_test")
                .name("Test Dress").dressType("BRIDAL")
                .retailPrice(new BigDecimal("85000.00"))
                .category(buildCategory())
                .isAvailable(true).isActive(true)
                .fulfillmentOptions(new ArrayList<>())
                .images(new ArrayList<>())
                .build();
    }

    private DressFulfillmentOption buildRentalOption(Dress dress) {
        return DressFulfillmentOption.builder()
                .dress(dress)
                .fulfillmentType("RENTAL")
                .rentalPricePerDay(PER_DAY)
                .rentalDeposit(DEPOSIT)
                .rentalPeriodDays(DAYS)
                .isActive(true)
                .build();
    }

    private Rental buildHandedOverRental(Dress dress, User customer) {
        return Rental.builder()
                .rentalId(10).publicId("rnt_test")
                .dress(dress).customer(customer).createdBy(buildAdmin())
                .nicNumber("123456789V")
                .rentalPricePerDay(PER_DAY)
                .rentalPeriodDays(DAYS)
                .depositAmount(DEPOSIT)
                .totalRentalFee(new BigDecimal("15000.00"))
                .totalPaidUpfront(new BigDecimal("25000.00"))
                .handedOverAt(LocalDateTime.now().minusDays(3))
                .dueDate(LocalDate.now())
                .status("HANDED_OVER")
                .damageItems(new ArrayList<>())
                .build();
    }

    private RentalCreateRequest buildCreateRequest() {
        return new RentalCreateRequest("drs_test", "usr_cust1", "123456789V");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validBooking_snapshotsPricingCorrectly() {
        Dress dress        = buildAvailableDress();
        User  createdBy    = buildAdmin();
        User  customer     = buildCustomer();
        DressFulfillmentOption rentalOption = buildRentalOption(dress);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(createdBy));
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(fulfillmentRepository.findByDress_DressIdAndFulfillmentType(1, "RENTAL"))
                .thenReturn(Optional.of(rentalOption));
        when(userRepository.findByPublicId("usr_cust1")).thenReturn(Optional.of(customer));
        when(publicIdGenerator.forRental()).thenReturn("rnt_newid");
        when(sanitizer.sanitizeText("123456789V")).thenReturn("123456789V");
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(buildCreateRequest(), "admin@test.com");

        verify(rentalRepository).save(rentalCaptor.capture());
        Rental saved = rentalCaptor.getValue();
        assertEquals(new BigDecimal("15000.00"), saved.getTotalRentalFee()); // 5000 x 3
    }

    @Test
    void create_totalPaidUpfront_equalsFeesPlusDeposit() {
        Dress dress        = buildAvailableDress();
        User  createdBy    = buildAdmin();
        User  customer     = buildCustomer();
        DressFulfillmentOption rentalOption = buildRentalOption(dress);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(createdBy));
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(fulfillmentRepository.findByDress_DressIdAndFulfillmentType(1, "RENTAL"))
                .thenReturn(Optional.of(rentalOption));
        when(userRepository.findByPublicId("usr_cust1")).thenReturn(Optional.of(customer));
        when(publicIdGenerator.forRental()).thenReturn("rnt_newid");
        when(sanitizer.sanitizeText(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(buildCreateRequest(), "admin@test.com");

        verify(rentalRepository).save(rentalCaptor.capture());
        Rental saved = rentalCaptor.getValue();
        assertEquals(new BigDecimal("25000.00"), saved.getTotalPaidUpfront()); // 15000 + 10000
    }

    @Test
    void create_dressNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(buildAdmin()));
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(buildCreateRequest(), "admin@test.com"));
    }

    @Test
    void create_dressNotAvailable_throwsIllegalStateException() {
        Dress unavailable = buildAvailableDress();
        unavailable.setIsAvailable(false);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(buildAdmin()));
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(unavailable));

        assertThrows(IllegalStateException.class,
                () -> service.create(buildCreateRequest(), "admin@test.com"));
    }

    @Test
    void create_dressHasNoRentalFulfillmentOption_throwsIllegalArgumentException() {
        Dress dress = buildAvailableDress();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(buildAdmin()));
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(fulfillmentRepository.findByDress_DressIdAndFulfillmentType(1, "RENTAL"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.create(buildCreateRequest(), "admin@test.com"));
    }

    @Test
    void create_customerNotFound_throwsResourceNotFoundException() {
        Dress dress        = buildAvailableDress();
        DressFulfillmentOption rentalOption = buildRentalOption(dress);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(buildAdmin()));
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(fulfillmentRepository.findByDress_DressIdAndFulfillmentType(1, "RENTAL"))
                .thenReturn(Optional.of(rentalOption));
        when(userRepository.findByPublicId("usr_cust1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(buildCreateRequest(), "admin@test.com"));
    }

    @Test
    void create_customerPublicIdBelongsToNonCustomer_throwsIllegalArgumentException() {
        Dress dress = buildAvailableDress();
        DressFulfillmentOption rentalOption = buildRentalOption(dress);
        User admin = buildAdmin(); // role = ADMIN, not CUSTOMER
        admin.setPublicId("usr_cust1"); // pretend the ID is being passed as customer

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(buildAdmin()));
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(fulfillmentRepository.findByDress_DressIdAndFulfillmentType(1, "RENTAL"))
                .thenReturn(Optional.of(rentalOption));
        when(userRepository.findByPublicId("usr_cust1")).thenReturn(Optional.of(admin));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(buildCreateRequest(), "admin@test.com"));
    }

    // ── handover ──────────────────────────────────────────────────────────────

    @Test
    void handover_bookedRental_setsHandedOverStatusAndMarksDressUnavailable() {
        Dress  dress   = buildAvailableDress();
        User   customer = buildCustomer();
        Rental rental  = Rental.builder()
                .rentalId(10).publicId("rnt_test")
                .dress(dress).customer(customer).createdBy(buildAdmin())
                .rentalPeriodDays(DAYS).status("BOOKED")
                .damageItems(new ArrayList<>()).build();

        when(rentalRepository.findByPublicId("rnt_test")).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RentalResponse result = service.handover("rnt_test");

        assertEquals("HANDED_OVER", result.status());
        assertFalse(dress.getIsAvailable());
        verify(dressRepository).save(dress);
    }

    @Test
    void handover_nonBookedRental_throwsIllegalStateException() {
        Dress  dress   = buildAvailableDress();
        Rental rental  = Rental.builder()
                .publicId("rnt_test").dress(dress).status("HANDED_OVER")
                .damageItems(new ArrayList<>()).build();

        when(rentalRepository.findByPublicId("rnt_test")).thenReturn(Optional.of(rental));

        assertThrows(IllegalStateException.class,
                () -> service.handover("rnt_test"));
    }

    // ── processReturn ─────────────────────────────────────────────────────────

    @Test
    void processReturn_onTimeNoDamage_returnsClean() {
        Dress  dress   = buildAvailableDress();
        dress.setIsAvailable(false);
        User   customer = buildCustomer();
        Rental rental  = buildHandedOverRental(dress, customer);
        // dueDate = today so return is on time

        when(rentalRepository.findByPublicId("rnt_test")).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RentalReturnRequest request = new RentalReturnRequest(null, null);
        RentalResponse result = service.processReturn("rnt_test", request);

        assertEquals("RETURNED_CLEAN", result.status());
        assertEquals(0, result.daysLate());
        assertTrue(BigDecimal.ZERO.compareTo(result.lateFine()) == 0,
                "Expected 0 but got " + result.lateFine());
        assertEquals(DEPOSIT, result.depositRefunded()); // full deposit back
        assertTrue(dress.getIsAvailable());
    }

    @Test
    void processReturn_lateOnly_statusReturnedLate_correctLateFine() {
        Dress  dress   = buildAvailableDress();
        dress.setIsAvailable(false);
        User   customer = buildCustomer();
        Rental rental  = buildHandedOverRental(dress, customer);
        rental.setDueDate(LocalDate.now().minusDays(2)); // 2 days overdue

        when(rentalRepository.findByPublicId("rnt_test")).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RentalReturnRequest request = new RentalReturnRequest(null, null);
        RentalResponse result = service.processReturn("rnt_test", request);

        assertEquals("RETURNED_LATE", result.status());
        assertEquals(2, result.daysLate());
        assertEquals(new BigDecimal("10000.00"), result.lateFine()); // 5000 x 2
    }

    @Test
    void processReturn_damagedOnly_statusReturnedDamaged_correctCosts() {
        Dress  dress   = buildAvailableDress();
        dress.setIsAvailable(false);
        User   customer = buildCustomer();
        Rental rental  = buildHandedOverRental(dress, customer);

        List<DamageItemRequest> items = List.of(
                new DamageItemRequest("Torn seam", new BigDecimal("3000.00"))
        );
        RentalReturnRequest request = new RentalReturnRequest(null, items);

        when(rentalRepository.findByPublicId("rnt_test")).thenReturn(Optional.of(rental));
        when(damageItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sanitizer.sanitizeText("Torn seam")).thenReturn("Torn seam");

        RentalResponse result = service.processReturn("rnt_test", request);

        assertEquals("RETURNED_DAMAGED", result.status());
        assertEquals(new BigDecimal("3000.00"), result.totalDamageCost());
        assertEquals(new BigDecimal("7000.00"), result.depositRefunded()); // 10000 - 3000
    }

    @Test
    void processReturn_lateAndDamaged_statusReturnedLateDamaged_sumsCorrectly() {
        Dress  dress   = buildAvailableDress();
        dress.setIsAvailable(false);
        User   customer = buildCustomer();
        Rental rental  = buildHandedOverRental(dress, customer);
        rental.setDueDate(LocalDate.now().minusDays(1)); // 1 day late = 5000 fine

        List<DamageItemRequest> items = List.of(
                new DamageItemRequest("Stain", new BigDecimal("2000.00"))
        );
        RentalReturnRequest request = new RentalReturnRequest(null, items);

        when(rentalRepository.findByPublicId("rnt_test")).thenReturn(Optional.of(rental));
        when(damageItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sanitizer.sanitizeText("Stain")).thenReturn("Stain");

        RentalResponse result = service.processReturn("rnt_test", request);

        assertEquals("RETURNED_LATE_DAMAGED", result.status());
        // totalDeductions = 5000 (late) + 2000 (damage) = 7000
        // depositRefunded = 10000 - 7000 = 3000
        assertEquals(new BigDecimal("7000.00"), result.totalDeductions());
        assertEquals(new BigDecimal("3000.00"), result.depositRefunded());
    }

    @Test
    void processReturn_deductionsExceedDeposit_statusBalanceDue() {
        Dress  dress   = buildAvailableDress();
        dress.setIsAvailable(false);
        User   customer = buildCustomer();
        Rental rental  = buildHandedOverRental(dress, customer);
        rental.setDueDate(LocalDate.now().minusDays(3)); // 3 days late = 15000 fine

        List<DamageItemRequest> items = List.of(
                new DamageItemRequest("Ripped", new BigDecimal("5000.00")) // damage = 5000
        );
        // totalDeductions = 15000 + 5000 = 20000, deposit = 10000
        // outstandingBalance = 20000 - 10000 = 10000
        RentalReturnRequest request = new RentalReturnRequest(null, items);

        when(rentalRepository.findByPublicId("rnt_test")).thenReturn(Optional.of(rental));
        when(damageItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sanitizer.sanitizeText("Ripped")).thenReturn("Ripped");

        RentalResponse result = service.processReturn("rnt_test", request);

        assertEquals("BALANCE_DUE", result.status());
        assertEquals(new BigDecimal("10000.00"), result.outstandingBalance());
        assertEquals(BigDecimal.ZERO, result.depositRefunded());
    }

    @Test
    void processReturn_dressFlipsBackToAvailable() {
        Dress  dress   = buildAvailableDress();
        dress.setIsAvailable(false);
        User   customer = buildCustomer();
        Rental rental  = buildHandedOverRental(dress, customer);

        when(rentalRepository.findByPublicId("rnt_test")).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processReturn("rnt_test", new RentalReturnRequest(null, null));

        assertTrue(dress.getIsAvailable());
        verify(dressRepository).save(dress);
    }

    @Test
    void processReturn_wrongStatus_throwsIllegalStateException() {
        Dress  dress  = buildAvailableDress();
        Rental rental = Rental.builder()
                .publicId("rnt_test").dress(dress).status("BOOKED")
                .damageItems(new ArrayList<>()).build();

        when(rentalRepository.findByPublicId("rnt_test")).thenReturn(Optional.of(rental));

        assertThrows(IllegalStateException.class,
                () -> service.processReturn("rnt_test", new RentalReturnRequest(null, null)));
    }

    // ── markOverdueRentals ────────────────────────────────────────────────────

    @Test
    void markOverdueRentals_twoOverdueRentals_setsStatusAndReturnsCount() {
        Dress  dress    = buildAvailableDress();
        User   customer = buildCustomer();

        Rental r1 = buildHandedOverRental(dress, customer);
        r1.setPublicId("rnt_1");
        r1.setDueDate(LocalDate.now().minusDays(2));

        Rental r2 = buildHandedOverRental(dress, customer);
        r2.setPublicId("rnt_2");
        r2.setDueDate(LocalDate.now().minusDays(1));

        when(rentalRepository.findOverdue(any(LocalDate.class))).thenReturn(List.of(r1, r2));
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.markOverdueRentals();

        assertEquals(2, count);
        assertEquals("OVERDUE", r1.getStatus());
        assertEquals("OVERDUE", r2.getStatus());
        verify(rentalRepository, times(2)).save(any(Rental.class));
    }

    @Test
    void markOverdueRentals_noOverdueRentals_returnsZeroAndNoSaves() {
        when(rentalRepository.findOverdue(any(LocalDate.class))).thenReturn(List.of());

        int count = service.markOverdueRentals();

        assertEquals(0, count);
        verify(rentalRepository, never()).save(any());
    }
}