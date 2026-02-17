package edu.bridalshop.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DressImageResponse {

    private Integer imageId;
    private String url;
    private Integer displayOrder;
}
