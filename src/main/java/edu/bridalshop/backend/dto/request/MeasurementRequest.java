package edu.bridalshop.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementRequest {

    @NotNull(message = "Customer ID is required")
    private Integer customerId;

    // All measurement fields optional (null = not measured yet)
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal heightWithShoes;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal hollowToHem;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal fullBust;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal underBust;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal naturalWaist;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal fullHip;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal shoulderWidth;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal torsoLength;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal thighCircumference;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal waistToKnee;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal waistToFloor;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal armhole;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal bicepCircumference;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal elbowCircumference;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal wristCircumference;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal sleeveLength;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal upperBust;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal bustApexDistance;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal shoulderToBustPoint;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal neckCircumference;
    @DecimalMin(value = "0.0", message = "Must be positive") private BigDecimal trainLength;

    private String notes;
}
