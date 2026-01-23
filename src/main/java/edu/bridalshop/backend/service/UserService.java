package edu.bridalshop.backend.service;

import edu.bridalshop.backend.dto.request.CreateEmployeeRequest;
import edu.bridalshop.backend.dto.response.MessageResponse;
import edu.bridalshop.backend.dto.response.UserResponse;
import edu.bridalshop.backend.entity.Employee;
import edu.bridalshop.backend.entity.User;
import edu.bridalshop.backend.enums.OAuthProvider;
import edu.bridalshop.backend.enums.UserRole;
import edu.bridalshop.backend.exception.EmailAlreadyExistsException;
import edu.bridalshop.backend.exception.ResourceNotFoundException;
import edu.bridalshop.backend.repository.EmployeeRepository;
import edu.bridalshop.backend.repository.UserRepository;
import edu.bridalshop.backend.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse createEmployee(CreateEmployeeRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Generate random password
        String tempPassword = PasswordGenerator.generateRandomPassword();

        // Create user account
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(UserRole.EMPLOYEE)
                .oauthProvider(OAuthProvider.EMAIL)
                .emailVerified(true) // Auto-verified for admin-created accounts
                .phoneVerified(true) // Auto-verified for admin-created accounts
                .profileCompleted(true)
                .passwordChangeRequired(true) // Notify to change password
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Create employee record
        Employee employee = Employee.builder()
                .user(user)
                .jobTitle(request.getJobTitle())
                .employmentType(request.getEmploymentType())
                .salaryType(request.getSalaryType())
                .baseSalary(request.getBaseSalary())
                .build();

        employeeRepository.save(employee);

        // Send welcome email with credentials
        emailService.sendEmployeeWelcomeEmail(
                user.getEmail(),
                user.getFullName(),
                tempPassword
        );

        log.info("Employee created: {} by admin", user.getEmail());

        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse createAdmin(CreateEmployeeRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Generate random password
        String tempPassword = PasswordGenerator.generateRandomPassword();

        // Create admin user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(UserRole.ADMIN)
                .oauthProvider(OAuthProvider.EMAIL)
                .emailVerified(true)
                .phoneVerified(true)
                .profileCompleted(true)
                .passwordChangeRequired(true)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Create employee record (admins are also employees)
        Employee employee = Employee.builder()
                .user(user)
                .jobTitle(request.getJobTitle())
                .employmentType(request.getEmploymentType())
                .salaryType(request.getSalaryType())
                .baseSalary(request.getBaseSalary())
                .build();

        employeeRepository.save(employee);

        // Send welcome email
        emailService.sendEmployeeWelcomeEmail(
                user.getEmail(),
                user.getFullName(),
                tempPassword
        );

        log.info("Admin created: {}", user.getEmail());

        return mapToUserResponse(user);
    }

    @Transactional
    public MessageResponse deactivateUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deactivated: {}", user.getEmail());

        return new MessageResponse("User deactivated successfully");
    }

    @Transactional
    public MessageResponse activateUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsActive(true);
        userRepository.save(user);

        log.info("User activated: {}", user.getEmail());

        return new MessageResponse("User activated successfully");
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .profileCompleted(user.getProfileCompleted())
                .passwordChangeRequired(user.getPasswordChangeRequired())
                .build();
    }
}
