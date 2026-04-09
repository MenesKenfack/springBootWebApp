package com.kaksha.library.service;

import com.kaksha.library.model.entity.Client;
import com.kaksha.library.model.entity.Librarian;
import com.kaksha.library.model.entity.Manager;
import com.kaksha.library.model.entity.User;
import com.kaksha.library.repository.ClientRepository;
import com.kaksha.library.repository.LibrarianRepository;
import com.kaksha.library.repository.ManagerRepository;
import com.kaksha.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final LibrarianRepository librarianRepository;
    private final ManagerRepository managerRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
        
        log.info("User found: {} with role: {}", user.getEmail(), user.getRole());
        String role = user.getRole().name();
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isActive(),
                true,
                true,
                user.isVerified(),
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
    
    public User findUserByEmail(String email) {
        log.info("Finding user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
        log.info("Found user: {} (ID: {}, Role: {}, Type: {})", 
            user.getEmail(), user.getUserID(), user.getRole(), user.getClass().getSimpleName());
        return user;
    }
    
    public Optional<Client> findClientByEmail(String email) {
        return clientRepository.findByEmail(email);
    }
    
    public Optional<Librarian> findLibrarianByEmail(String email) {
        return librarianRepository.findByEmail(email);
    }
    
    public Optional<Manager> findManagerByEmail(String email) {
        return managerRepository.findByEmail(email);
    }
}
