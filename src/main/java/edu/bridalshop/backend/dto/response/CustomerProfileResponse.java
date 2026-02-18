package edu.bridalshop.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerProfileResponse {

    // Customer details
    private Integer userId;
    private String  fullName;
    private String  email;
    private String  phone;

    // Latest measurement (null if not recorded yet)
    private MeasurementResponse latestMeasurement;
}
