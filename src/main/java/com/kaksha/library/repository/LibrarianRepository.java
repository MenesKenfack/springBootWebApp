package com.kaksha.library.repository;

import com.kaksha.library.model.entity.Librarian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LibrarianRepository extends JpaRepository<Librarian, Long> {
    
    Optional<Librarian> findByEmail(String email);
    
    Optional<Librarian> findByUsername(String username);
}
