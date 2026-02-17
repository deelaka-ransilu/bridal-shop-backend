package edu.bridalshop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DressImageRequest {

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "URL must be less than 500 characters")
    private String url;

    private Integer displayOrder = 0;
}
