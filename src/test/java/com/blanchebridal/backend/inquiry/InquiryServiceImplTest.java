package com.blanchebridal.backend.inquiry;

import com.blanchebridal.backend.exception.ResourceNotFoundException;
import com.blanchebridal.backend.inquiry.dto.req.CreateInquiryRequest;
import com.blanchebridal.backend.inquiry.dto.res.InquiryResponse;
import com.blanchebridal.backend.inquiry.entity.Inquiry;
import com.blanchebridal.backend.inquiry.entity.InquiryStatus;
import com.blanchebridal.backend.inquiry.repository.InquiryRepository;
import com.blanchebridal.backend.inquiry.service.impl.InquiryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryServiceImpl Tests")
class InquiryServiceImplTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @InjectMocks
    private InquiryServiceImpl inquiryService;

    private Inquiry inquiry;
    private UUID inquiryId;

    @BeforeEach
    void setUp() {
        inquiryId = UUID.randomUUID();
        inquiry = Inquiry.builder()
                .id(inquiryId)
                .name("Amaya Silva")
                .email("amaya@example.com")
                .phone("0771234567")
                .subject("Dress alteration")
                .message("I would like to enquire about altering my wedding dress.")
                .imageUrl("https://res.cloudinary.com/test/image.jpg")
                .status(InquiryStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── submitInquiry ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("submitInquiry — saves inquiry with status OPEN and returns response")
    void submitInquiry_savesWithStatusOpen() {
        CreateInquiryRequest req = new CreateInquiryRequest();
        req.setName("Amaya Silva");
        req.setEmail("amaya@example.com");
        req.setPhone("0771234567");
        req.setSubject("Dress alteration");
        req.setMessage("I would like to enquire about altering my wedding dress.");
        req.setImageUrl("https://res.cloudinary.com/test/image.jpg");

        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(inquiry);

        InquiryResponse response = inquiryService.submitInquiry(req);

        assertThat(response.getName()).isEqualTo("Amaya Silva");
        assertThat(response.getEmail()).isEqualTo("amaya@example.com");
        assertThat(response.getStatus()).isEqualTo(InquiryStatus.OPEN);
        assertThat(response.getImageUrl()).isEqualTo("https://res.cloudinary.com/test/image.jpg");
        verify(inquiryRepository, times(1)).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("submitInquiry — saves inquiry with optional fields null")
    void submitInquiry_withNullOptionalFields() {
        Inquiry minimalInquiry = Inquiry.builder()
                .id(UUID.randomUUID())
                .name("Nimal Perera")
                .email("nimal@example.com")
                .message("Simple enquiry.")
                .status(InquiryStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        CreateInquiryRequest req = new CreateInquiryRequest();
        req.setName("Nimal Perera");
        req.setEmail("nimal@example.com");
        req.setMessage("Simple enquiry.");

        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(minimalInquiry);

        InquiryResponse response = inquiryService.submitInquiry(req);

        assertThat(response.getName()).isEqualTo("Nimal Perera");
        assertThat(response.getPhone()).isNull();
        assertThat(response.getSubject()).isNull();
        assertThat(response.getImageUrl()).isNull();
        assertThat(response.getStatus()).isEqualTo(InquiryStatus.OPEN);
    }

    // ── getAllInquiries ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllInquiries — returns all inquiries when status is null")
    void getAllInquiries_noStatusFilter_returnsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry));

        when(inquiryRepository.findAll(pageable)).thenReturn(page);

        Page<InquiryResponse> result = inquiryService.getAllInquiries(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(inquiryId);
        verify(inquiryRepository).findAll(pageable);
        verify(inquiryRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("getAllInquiries — filters by status when status is provided")
    void getAllInquiries_withStatusFilter_returnsFiltered() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry));

        when(inquiryRepository.findByStatus(InquiryStatus.OPEN, pageable)).thenReturn(page);

        Page<InquiryResponse> result = inquiryService.getAllInquiries(InquiryStatus.OPEN, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(InquiryStatus.OPEN);
        verify(inquiryRepository).findByStatus(InquiryStatus.OPEN, pageable);
        verify(inquiryRepository, never()).findAll(pageable);
    }

    @Test
    @DisplayName("getAllInquiries — returns empty page when no inquiries found")
    void getAllInquiries_empty_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(inquiryRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<InquiryResponse> result = inquiryService.getAllInquiries(null, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ── getInquiryById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getInquiryById — returns response when inquiry exists")
    void getInquiryById_found_returnsResponse() {
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry));

        InquiryResponse response = inquiryService.getInquiryById(inquiryId);

        assertThat(response.getId()).isEqualTo(inquiryId);
        assertThat(response.getEmail()).isEqualTo("amaya@example.com");
    }

    @Test
    @DisplayName("getInquiryById — throws ResourceNotFoundException when not found")
    void getInquiryById_notFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(inquiryRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.getInquiryById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus — updates from OPEN to IN_PROGRESS")
    void updateStatus_openToInProgress_updatesCorrectly() {
        Inquiry updated = Inquiry.builder()
                .id(inquiryId)
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .message(inquiry.getMessage())
                .status(InquiryStatus.IN_PROGRESS)
                .createdAt(inquiry.getCreatedAt())
                .build();

        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(updated);

        InquiryResponse response = inquiryService.updateStatus(inquiryId, InquiryStatus.IN_PROGRESS);

        assertThat(response.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
        verify(inquiryRepository).save(inquiry);
    }

    @Test
    @DisplayName("updateStatus — updates from IN_PROGRESS to RESOLVED")
    void updateStatus_inProgressToResolved_updatesCorrectly() {
        inquiry.setStatus(InquiryStatus.IN_PROGRESS);

        Inquiry resolved = Inquiry.builder()
                .id(inquiryId)
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .message(inquiry.getMessage())
                .status(InquiryStatus.RESOLVED)
                .createdAt(inquiry.getCreatedAt())
                .build();

        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(resolved);

        InquiryResponse response = inquiryService.updateStatus(inquiryId, InquiryStatus.RESOLVED);

        assertThat(response.getStatus()).isEqualTo(InquiryStatus.RESOLVED);
    }

    @Test
    @DisplayName("updateStatus — throws ResourceNotFoundException when inquiry not found")
    void updateStatus_notFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(inquiryRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.updateStatus(unknownId, InquiryStatus.RESOLVED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());

        verify(inquiryRepository, never()).save(any());
    }
}