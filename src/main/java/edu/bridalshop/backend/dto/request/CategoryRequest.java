package edu.bridalshop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Dress type is required")
        @Pattern(regexp = "BRIDAL|PARTY", message = "Dress type must be BRIDAL or PARTY")
        String dressType,

        @Size(max = 2000, message = "Description is too long")
        String description
) {}