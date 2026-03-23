package edu.bridalshop.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class UserResponse {
    private String publicId;
    private String fullName;
    private String email;
    private String role;
    private String profilePicture;
    private Boolean emailVerified;
    private Boolean profileCompleted;
}