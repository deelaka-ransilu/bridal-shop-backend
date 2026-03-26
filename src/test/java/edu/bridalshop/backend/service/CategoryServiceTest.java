package edu.bridalshop.backend.service;

import edu.bridalshop.backend.dto.request.CategoryRequest;
import edu.bridalshop.backend.dto.response.CategoryResponse;
import edu.bridalshop.backend.entity.Category;
import edu.bridalshop.backend.exception.ResourceNotFoundException;
import edu.bridalshop.backend.repository.CategoryRepository;
import edu.bridalshop.backend.util.PayloadSanitizer;
import edu.bridalshop.backend.util.PublicIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock PublicIdGenerator   publicIdGenerator;
    @Mock PayloadSanitizer    sanitizer;

    @InjectMocks CategoryService service;

    @Captor ArgumentCaptor<Category> categoryCaptor;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Category buildCategory() {
        return Category.builder()
                .categoryId(1).publicId("cat_test")
                .name("Ballgown").dressType("BRIDAL").isActive(true).build();
    }

    private CategoryRequest buildRequest() {
        return new CategoryRequest("Ballgown", "BRIDAL", "Elegant ballgown category");
    }

    // ── getAllActive ──────────────────────────────────────────────────────────

    @Test
    void getAllActive_returnsActiveCategoryList() {
        Category cat = buildCategory();
        when(categoryRepository.findAllByIsActiveTrue()).thenReturn(List.of(cat));

        List<CategoryResponse> result = service.getAllActive();

        assertEquals(1, result.size());
        assertEquals("cat_test", result.get(0).publicId());
        verify(categoryRepository).findAllByIsActiveTrue();
    }

    @Test
    void getAllActive_noActiveCategories_returnsEmptyList() {
        when(categoryRepository.findAllByIsActiveTrue()).thenReturn(List.of());

        List<CategoryResponse> result = service.getAllActive();

        assertTrue(result.isEmpty());
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_noDuplicate_returnsSavedResponse() {
        CategoryRequest request = buildRequest();

        when(sanitizer.sanitizeText("Ballgown")).thenReturn("Ballgown");
        when(categoryRepository.existsByNameAndDressType("Ballgown", "BRIDAL")).thenReturn(false);
        when(publicIdGenerator.forCategory()).thenReturn("cat_test123456");
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse result = service.create(request);

        assertNotNull(result.publicId());
        assertEquals("Ballgown", result.name());
        assertEquals("BRIDAL", result.dressType());
        verify(categoryRepository).save(categoryCaptor.capture());
        assertEquals("cat_test123456", categoryCaptor.getValue().getPublicId());
        assertTrue(categoryCaptor.getValue().getIsActive());
    }

    @Test
    void create_duplicateNameAndType_throwsIllegalArgumentException() {
        CategoryRequest request = buildRequest();

        when(sanitizer.sanitizeText("Ballgown")).thenReturn("Ballgown");
        when(categoryRepository.existsByNameAndDressType("Ballgown", "BRIDAL")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(request));
        verify(categoryRepository, never()).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_validRequest_nameChanged_noConflict_returnsUpdatedResponse() {
        Category existing = buildCategory(); // name="Ballgown", type="BRIDAL"
        CategoryRequest request = new CategoryRequest("A-Line", "BRIDAL", "Updated desc");

        when(categoryRepository.findByPublicId("cat_test")).thenReturn(Optional.of(existing));
        when(sanitizer.sanitizeText("A-Line")).thenReturn("A-Line");
        when(categoryRepository.existsByNameAndDressType("A-Line", "BRIDAL")).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse result = service.update("cat_test", request);

        assertEquals("A-Line", result.name());
        verify(categoryRepository).save(any());
    }

    @Test
    void update_nameUnchanged_skipsDuplicateCheck_updatesSuccessfully() {
        Category existing = buildCategory(); // name="Ballgown", type="BRIDAL"
        CategoryRequest request = new CategoryRequest("Ballgown", "BRIDAL", "New desc");

        when(categoryRepository.findByPublicId("cat_test")).thenReturn(Optional.of(existing));
        when(sanitizer.sanitizeText("Ballgown")).thenReturn("Ballgown");
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update("cat_test", request);

        // existsByNameAndDressType should NOT be called when name/type are unchanged
        verify(categoryRepository, never()).existsByNameAndDressType(any(), any());
        verify(categoryRepository).save(any());
    }

    @Test
    void update_publicIdNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findByPublicId("cat_notexist")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> service.update("cat_notexist", buildRequest())
        );
        assertTrue(ex.getMessage().contains("cat_notexist"));
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Test
    void deactivate_validPublicId_setsIsActiveFalseAndSaves() {
        Category category = buildCategory();
        when(categoryRepository.findByPublicId("cat_test")).thenReturn(Optional.of(category));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate("cat_test");

        verify(categoryRepository).save(categoryCaptor.capture());
        assertFalse(categoryCaptor.getValue().getIsActive());
    }

    @Test
    void deactivate_publicIdNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findByPublicId("cat_notexist")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> service.deactivate("cat_notexist")
        );
        assertTrue(ex.getMessage().contains("cat_notexist"));
    }
}