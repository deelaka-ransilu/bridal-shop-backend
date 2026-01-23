package edu.bridalshop.backend.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import edu.bridalshop.backend.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private UserRole role;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean profileCompleted;
    private Boolean passwordChangeRequired;
}
