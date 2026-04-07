package com.kaksha.library.repository;

import com.kaksha.library.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmailAndVerificationCode(String email, String verificationCode);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
    
    // Dashboard stats methods
    long countByVerifiedFalse();
    
    long countByVerifiedTrue();
}
