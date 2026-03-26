package edu.bridalshop.backend.service;

import edu.bridalshop.backend.dto.request.DressRequest;
import edu.bridalshop.backend.dto.request.FulfillmentOptionRequest;
import edu.bridalshop.backend.dto.response.DressImageResponse;
import edu.bridalshop.backend.dto.response.DressResponse;
import edu.bridalshop.backend.entity.*;
import edu.bridalshop.backend.exception.ResourceNotFoundException;
import edu.bridalshop.backend.repository.*;
import edu.bridalshop.backend.util.PayloadSanitizer;
import edu.bridalshop.backend.util.PublicIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DressServiceTest {

    @Mock DressRepository                     dressRepository;
    @Mock DressFulfillmentOptionRepository    fulfillmentRepository;
    @Mock DressImageRepository                imageRepository;
    @Mock CategoryRepository                  categoryRepository;
    @Mock UserRepository                      userRepository;
    @Mock CloudinaryService                   cloudinaryService;
    @Mock PublicIdGenerator                   publicIdGenerator;
    @Mock PayloadSanitizer                    sanitizer;

    @InjectMocks DressService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Category buildCategory() {
        return Category.builder()
                .categoryId(1).publicId("cat_test")
                .name("Ballgown").dressType("BRIDAL").isActive(true).build();
    }

    private Dress buildDress() {
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

    private DressRequest buildDressRequest(List<FulfillmentOptionRequest> options) {
        return new DressRequest(
                "cat_test", "Test Dress", "A description",
                "BRIDAL", new BigDecimal("85000.00"),
                "Satin", "White", null, options
        );
    }

    private FulfillmentOptionRequest buildRentalOption() {
        return new FulfillmentOptionRequest(
                "RENTAL", null,
                new BigDecimal("5000.00"), new BigDecimal("10000.00"), 3
        );
    }

    private MultipartFile buildMockFile() {
        return new MockMultipartFile(
                "file", "dress.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );
    }

    // ── getCatalog ────────────────────────────────────────────────────────────

    @Test
    void getCatalog_noFilters_callsFindCatalogDresses() {
        Dress dress = buildDress();
        when(dressRepository.findCatalogDresses()).thenReturn(List.of(dress));

        List<DressResponse> result = service.getCatalog(null, null, null);

        assertEquals(1, result.size());
        assertNull(result.get(0).retailPrice()); // price hidden for public
        verify(dressRepository).findCatalogDresses();
    }

    @Test
    void getCatalog_filterByCategory_callsCorrectRepository() {
        when(dressRepository.findCatalogDressesByCategory("cat_test")).thenReturn(List.of());

        service.getCatalog("cat_test", null, null);

        verify(dressRepository).findCatalogDressesByCategory("cat_test");
        verify(dressRepository, never()).findCatalogDresses();
    }

    @Test
    void getCatalog_filterByDressType_callsCorrectRepository() {
        when(dressRepository.findCatalogDressesByType("BRIDAL")).thenReturn(List.of());

        service.getCatalog(null, "bridal", null);

        verify(dressRepository).findCatalogDressesByType("BRIDAL");
    }

    @Test
    void getFeatured_returnsDressListWithNullPrices() {
        Dress dress = buildDress();
        when(dressRepository.findFeaturedDresses()).thenReturn(List.of(dress));

        List<DressResponse> result = service.getFeatured();

        assertFalse(result.isEmpty());
        assertNull(result.get(0).retailPrice());
        verify(dressRepository).findFeaturedDresses();
    }

    // ── getByPublicId ─────────────────────────────────────────────────────────

    @Test
    void getByPublicId_unauthenticated_priceIsHidden() {
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(buildDress()));

        DressResponse result = service.getByPublicId("drs_test", false);

        assertNull(result.retailPrice());
    }

    @Test
    void getByPublicId_authenticated_priceIsVisible() {
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(buildDress()));

        DressResponse result = service.getByPublicId("drs_test", true);

        assertNotNull(result.retailPrice());
        assertEquals(new BigDecimal("85000.00"), result.retailPrice());
    }

    @Test
    void getByPublicId_notFound_throwsResourceNotFoundException() {
        when(dressRepository.findByPublicId("drs_notexist")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getByPublicId("drs_notexist", true));
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validRequestWithRentalOption_savesDressAndFulfillment() {
        DressRequest request = buildDressRequest(List.of(buildRentalOption()));

        when(categoryRepository.findByPublicId("cat_test")).thenReturn(Optional.of(buildCategory()));
        when(sanitizer.sanitizeText("Test Dress")).thenReturn("Test Dress");
        when(sanitizer.sanitizeRichText("A description")).thenReturn("A description");
        when(publicIdGenerator.forDress()).thenReturn("drs_newid");
        when(dressRepository.save(any())).thenAnswer(inv -> {
            Dress d = inv.getArgument(0);
            d.setDressId(1);
            return d;
        });
        when(fulfillmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DressResponse result = service.create(request);

        assertNotNull(result);
        verify(dressRepository).save(any(Dress.class));
        verify(fulfillmentRepository).save(any(DressFulfillmentOption.class));
    }

    @Test
    void create_categoryNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findByPublicId("cat_test")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(buildDressRequest(List.of(buildRentalOption()))));
    }

    @Test
    void create_rentalOptionMissingRentalPricePerDay_throwsIllegalArgumentException() {
        FulfillmentOptionRequest badOption = new FulfillmentOptionRequest(
                "RENTAL", null, null, new BigDecimal("10000.00"), 3
        );
        DressRequest request = buildDressRequest(List.of(badOption));

        when(categoryRepository.findByPublicId("cat_test")).thenReturn(Optional.of(buildCategory()));
        when(sanitizer.sanitizeText(any())).thenAnswer(inv -> inv.getArgument(0));
        when(publicIdGenerator.forDress()).thenReturn("drs_x");
        when(dressRepository.save(any())).thenAnswer(inv -> {
            Dress d = inv.getArgument(0); d.setDressId(1); return d;
        });

        assertThrows(IllegalArgumentException.class, () -> service.create(request));
    }

    @Test
    void create_rentalOptionMissingRentalDeposit_throwsIllegalArgumentException() {
        FulfillmentOptionRequest badOption = new FulfillmentOptionRequest(
                "RENTAL", null, new BigDecimal("5000.00"), null, 3
        );
        DressRequest request = buildDressRequest(List.of(badOption));

        when(categoryRepository.findByPublicId("cat_test")).thenReturn(Optional.of(buildCategory()));
        when(sanitizer.sanitizeText(any())).thenAnswer(inv -> inv.getArgument(0));
        when(publicIdGenerator.forDress()).thenReturn("drs_x");
        when(dressRepository.save(any())).thenAnswer(inv -> {
            Dress d = inv.getArgument(0); d.setDressId(1); return d;
        });

        assertThrows(IllegalArgumentException.class, () -> service.create(request));
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Test
    void deactivate_validPublicId_setsIsActiveFalse() {
        Dress dress = buildDress();
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(dressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate("drs_test");

        assertFalse(dress.getIsActive());
        verify(dressRepository).save(dress);
    }

    // ── uploadImage ───────────────────────────────────────────────────────────

    @Test
    void uploadImage_underLimit_uploadsAndSavesImage() throws IOException {
        Dress dress = buildDress();
        MultipartFile file = buildMockFile();
        String cloudUrl = "https://res.cloudinary.com/demo/image/upload/v1/dress.jpg";

        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(imageRepository.countByDress_DressId(1)).thenReturn(2);
        when(cloudinaryService.uploadDressImage(file)).thenReturn(cloudUrl);
        when(publicIdGenerator.forImage()).thenReturn("img_test");
        when(imageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DressImageResponse result = service.uploadImage("drs_test", file);

        assertNotNull(result);
        verify(imageRepository).save(any(DressImage.class));
    }

    @Test
    void uploadImage_alreadyAtMaxLimit_throwsIllegalStateException() {
        Dress dress = buildDress();
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(imageRepository.countByDress_DressId(1)).thenReturn(5);

        assertThrows(IllegalStateException.class,
                () -> service.uploadImage("drs_test", buildMockFile()));
    }

    @Test
    void uploadImage_firstImageUploaded_isPrimaryTrue() throws IOException {
        Dress dress = buildDress();
        MultipartFile file = buildMockFile();
        String cloudUrl = "https://res.cloudinary.com/demo/image/upload/v1/dress.jpg";

        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(imageRepository.countByDress_DressId(1)).thenReturn(0);
        when(cloudinaryService.uploadDressImage(file)).thenReturn(cloudUrl);
        when(publicIdGenerator.forImage()).thenReturn("img_first");
        when(imageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DressImageResponse result = service.uploadImage("drs_test", file);

        assertTrue(result.isPrimary());
    }

    // ── deleteImage ───────────────────────────────────────────────────────────

    @Test
    void deleteImage_deletePrimaryImage_promotesNextImageToPrimary() {
        Dress dress = buildDress();

        DressImage primaryImage = DressImage.builder()
                .publicId("img_primary").dress(dress).isPrimary(true).displayOrder(0).build();
        DressImage nextImage = DressImage.builder()
                .publicId("img_next").dress(dress).isPrimary(false).displayOrder(1).build();

        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(imageRepository.findByPublicId("img_primary")).thenReturn(Optional.of(primaryImage));
        when(imageRepository.findAllByDress_DressIdOrderByDisplayOrderAsc(1))
                .thenReturn(List.of(nextImage));
        when(imageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteImage("drs_test", "img_primary");

        assertTrue(nextImage.getIsPrimary());
        verify(imageRepository).save(nextImage);
    }

    @Test
    void deleteImage_deleteNonPrimaryImage_doesNotPromoteAnother() {
        Dress dress = buildDress();
        DressImage nonPrimary = DressImage.builder()
                .publicId("img_second").dress(dress).isPrimary(false).displayOrder(1).build();

        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(dress));
        when(imageRepository.findByPublicId("img_second")).thenReturn(Optional.of(nonPrimary));

        service.deleteImage("drs_test", "img_second");

        verify(imageRepository).delete(nonPrimary);
        // findAllByDress_DressIdOrderByDisplayOrderAsc should NOT be called
        verify(imageRepository, never()).findAllByDress_DressIdOrderByDisplayOrderAsc(any());
    }

    @Test
    void deleteImage_imageNotFound_throwsResourceNotFoundException() {
        when(dressRepository.findByPublicId("drs_test")).thenReturn(Optional.of(buildDress()));
        when(imageRepository.findByPublicId("img_notexist")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteImage("drs_test", "img_notexist"));
    }
}