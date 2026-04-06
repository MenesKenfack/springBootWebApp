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
        log.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
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
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
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
