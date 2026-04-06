package com.kaksha.library.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:noreply@kaksha.com}")
    private String fromEmail;
    
    @Value("${app.name:Kaksha Digital Library}")
    private String appName;
    
    public void sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Verify Your Email - " + appName);
            message.setText(buildVerificationEmailBody(verificationCode));
            
            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset - " + appName);
            message.setText(buildPasswordResetEmailBody(resetToken));
            
            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    public void sendPaymentConfirmationEmail(String toEmail, String resourceTitle, String amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Payment Confirmation - " + appName);
            message.setText(buildPaymentConfirmationEmailBody(resourceTitle, amount));
            
            mailSender.send(message);
            log.info("Payment confirmation email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send payment confirmation email to: {}", toEmail, e);
        }
    }
    
    public void sendLibrarianCredentialsEmail(String toEmail, String password, String firstName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Your Librarian Account - " + appName);
            message.setText(buildLibrarianCredentialsEmailBody(toEmail, password, firstName));
            
            mailSender.send(message);
            log.info("Librarian credentials email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send librarian credentials email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send librarian credentials email", e);
        }
    }
    
    private String buildVerificationEmailBody(String code) {
        return String.format(
            "Welcome to %s!\n\n" +
            "Your verification code is: %s\n\n" +
            "Please enter this code to verify your email address.\n\n" +
            "This code will expire in 30 minutes.\n\n" +
            "If you did not create an account, please ignore this email.\n\n" +
            "Best regards,\n" +
            "The %s Team",
            appName, code, appName
        );
    }
    
    private String buildPasswordResetEmailBody(String token) {
        return String.format(
            "Hello,\n\n" +
            "You requested a password reset for your %s account.\n\n" +
            "Your password reset token is: %s\n\n" +
            "This token will expire in 1 hour.\n\n" +
            "If you did not request this reset, please ignore this email.\n\n" +
            "Best regards,\n" +
            "The %s Team",
            appName, token, appName
        );
    }
    
    private String buildPaymentConfirmationEmailBody(String resourceTitle, String amount) {
        return String.format(
            "Hello,\n\n" +
            "Thank you for your purchase on %s!\n\n" +
            "Resource: %s\n" +
            "Amount: %s\n\n" +
            "You now have full access to this resource.\n\n" +
            "Best regards,\n" +
            "The %s Team",
            appName, resourceTitle, amount, appName
        );
    }
    
    private String buildLibrarianCredentialsEmailBody(String email, String password, String firstName) {
        return String.format(
            "Hello %s,\n\n" +
            "Welcome to %s! Your librarian account has been created by the system manager.\n\n" +
            "Here are your login credentials:\n\n" +
            "Email: %s\n" +
            "Password: %s\n\n" +
            "You can log in at: https://kaksha.com/login\n\n" +
            "For security reasons, please change your password after your first login.\n\n" +
            "Best regards,\n" +
            "The %s Team",
            firstName, appName, email, password, appName
        );
    }
}
