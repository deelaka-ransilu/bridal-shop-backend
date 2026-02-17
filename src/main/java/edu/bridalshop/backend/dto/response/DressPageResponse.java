package edu.bridalshop.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DressPageResponse {

    private List<DressResponse> dresses;
    private Integer currentPage;
    private Integer totalPages;
    private Long totalItems;
    private Integer pageSize;
}