package edu.bridalshop.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.verification-url}")
    private String verificationUrl;

    @Value("${app.email.reset-password-url}")
    private String resetPasswordUrl;

    @Async
    public void sendEmailVerification(String toEmail, String token) {
        try {
            String subject = "Verify Your Email - Bridal Shop";
            String verifyLink = verificationUrl + "?token=" + token;

            String body = String.format("""
                    Hello,
                    
                    Thank you for registering with Bridal Shop!
                    
                    Please click the link below to verify your email address:
                    %s
                    
                    This link will expire in 24 hours.
                    
                    If you didn't create an account, please ignore this email.
                    
                    Best regards,
                    Bridal Shop Team
                    """, verifyLink);

            sendEmail(toEmail, subject, body);
            log.info("Email verification sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            String subject = "Reset Your Password - Bridal Shop";
            String resetLink = resetPasswordUrl + "?token=" + token;

            String body = String.format("""
                    Hello,
                    
                    You requested to reset your password for your Bridal Shop account.
                    
                    Please click the link below to reset your password:
                    %s
                    
                    This link will expire in 1 hour.
                    
                    If you didn't request a password reset, please ignore this email.
                    
                    Best regards,
                    Bridal Shop Team
                    """, resetLink);

            sendEmail(toEmail, subject, body);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendEmployeeWelcomeEmail(String toEmail, String fullName, String tempPassword) {
        try {
            String subject = "Welcome to Bridal Shop - Employee Account Created";

            String body = String.format("""
                    Hello %s,
                    
                    Your employee account has been created at Bridal Shop!
                    
                    Login Credentials:
                    Email: %s
                    Temporary Password: %s
                    
                    Please login and change your password as soon as possible.
                    
                    Login URL: http://localhost:3000/employee/login
                    
                    Best regards,
                    Bridal Shop Team
                    """, fullName, toEmail, tempPassword);

            sendEmail(toEmail, subject, body);
            log.info("Welcome email sent to employee: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}
