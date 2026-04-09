package com.kaksha.library.config;

import com.kaksha.library.model.entity.Client;
import com.kaksha.library.model.entity.Librarian;
import com.kaksha.library.model.entity.Manager;
import com.kaksha.library.model.enums.UserRole;
import com.kaksha.library.model.enums.UserTier;
import com.kaksha.library.repository.ClientRepository;
import com.kaksha.library.repository.LibrarianRepository;
import com.kaksha.library.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

/**
 * Initializes test data on application startup.
 * Creates default admin, librarian, and client accounts if they don't exist.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final ClientRepository clientRepository;
    private final LibrarianRepository librarianRepository;
    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            try {
                initManager();
                initLibrarian();
                initClient();
            } catch (Exception e) {
                log.warn("Data initialization failed (database may not be ready yet): {}", e.getMessage());
                // Don't rethrow - allow application to start even if init fails
            }
        };
    }

    private void initManager() {
        managerRepository.findByEmail("admin@kaksha.com").ifPresentOrElse(
            manager -> {
                if (!manager.isActive()) {
                    manager.setActive(true);
                    managerRepository.save(manager);
                    log.info("Activated existing manager account: admin@kaksha.com");
                }
            },
            () -> {
                Manager manager = Manager.builder()
                        .username("admin")
                        .firstName("Admin")
                        .lastName("User")
                        .email("admin@kaksha.com")
                        .password(passwordEncoder.encode("admin123"))
                        .dateOfBirth(LocalDate.of(1990, 1, 1))
                        .role(UserRole.ROLE_MANAGER)
                        .userTier(UserTier.PREMIUM)
                        .verified(true)
                        .active(true)
                        .department("Management")
                        .build();
                managerRepository.save(java.util.Objects.requireNonNull(manager, "Manager cannot be null"));
                log.info("Created default manager account: admin@kaksha.com / admin123");
            }
        );
    }

    private void initLibrarian() {
        librarianRepository.findByEmail("librarian@kaksha.com").ifPresentOrElse(
            librarian -> {
                if (!librarian.isActive()) {
                    librarian.setActive(true);
                    librarianRepository.save(librarian);
                    log.info("Activated existing librarian account: librarian@kaksha.com");
                }
            },
            () -> {
                Librarian librarian = Librarian.builder()
                        .username("librarian")
                        .firstName("Library")
                        .lastName("Staff")
                        .email("librarian@kaksha.com")
                        .password(passwordEncoder.encode("lib123"))
                        .dateOfBirth(LocalDate.of(1995, 5, 15))
                        .role(UserRole.ROLE_LIBRARIAN)
                        .userTier(UserTier.PREMIUM)
                        .verified(true)
                        .active(true)
                        .department("General")
                        .employeeId("LIB001")
                        .build();
                librarianRepository.save(java.util.Objects.requireNonNull(librarian, "Librarian cannot be null"));
                log.info("Created default librarian account: librarian@kaksha.com / lib123");
            }
        );
    }

    private void initClient() {
        clientRepository.findByEmail("client@kaksha.com").ifPresentOrElse(
            client -> {
                if (!client.isActive()) {
                    client.setActive(true);
                    clientRepository.save(client);
                    log.info("Activated existing client account: client@kaksha.com");
                }
            },
            () -> {
                Client client = Client.builder()
                        .username("client")
                        .firstName("Test")
                        .lastName("Client")
                        .email("client@kaksha.com")
                        .password(passwordEncoder.encode("client123"))
                        .dateOfBirth(LocalDate.of(2000, 8, 20))
                        .role(UserRole.ROLE_CLIENT)
                        .userTier(UserTier.BASIC)
                        .verified(true)
                        .active(true)
                        .build();
                clientRepository.save(java.util.Objects.requireNonNull(client, "Client cannot be null"));
                log.info("Created default client account: client@kaksha.com / client123");
            }
        );
    }
}
