package edu.bridalshop.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeasurementResponse {

    private Integer measurementId;
    private Integer customerId;
    private String  customerName;
    private Integer recordedByUserId;
    private String  recordedByName;

    // All measurements (cm)
    private BigDecimal heightWithShoes;
    private BigDecimal hollowToHem;
    private BigDecimal fullBust;
    private BigDecimal underBust;
    private BigDecimal naturalWaist;
    private BigDecimal fullHip;
    private BigDecimal shoulderWidth;
    private BigDecimal torsoLength;
    private BigDecimal thighCircumference;
    private BigDecimal waistToKnee;
    private BigDecimal waistToFloor;
    private BigDecimal armhole;
    private BigDecimal bicepCircumference;
    private BigDecimal elbowCircumference;
    private BigDecimal wristCircumference;
    private BigDecimal sleeveLength;
    private BigDecimal upperBust;
    private BigDecimal bustApexDistance;
    private BigDecimal shoulderToBustPoint;
    private BigDecimal neckCircumference;
    private BigDecimal trainLength;

    private String          notes;
    private Boolean         isActive;
    private LocalDateTime   createdAt;
}
