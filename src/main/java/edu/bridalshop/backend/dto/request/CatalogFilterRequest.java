package edu.bridalshop.backend.dto.request;

import edu.bridalshop.backend.enums.SortBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogFilterRequest {

    // Filter parameters
    private Integer categoryId;
    private List<String> colors;
    private List<String> sizes;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean availableForSale;
    private Boolean availableForRental;
    private String searchQuery;

    // Sorting
    private SortBy sortBy = SortBy.NEWEST;

    // Pagination
    private Integer page = 0;
    private Integer size = 20;
}
