package edu.bridalshop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DressVariantRequest {

    @NotBlank(message = "Size is required")
    @Size(max = 20, message = "Size must be less than 20 characters")
    private String size;

    @NotBlank(message = "Color is required")
    @Size(max = 50, message = "Color must be less than 50 characters")
    private String color;

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must be less than 50 characters")
    private String sku;
}

