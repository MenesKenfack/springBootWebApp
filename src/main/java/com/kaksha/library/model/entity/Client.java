package com.kaksha.library.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "clients")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Client extends User {
    
    @Builder.Default
    @Column(nullable = false)
    private boolean status = true;
    
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<Payments> payments;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "client_resources",
        joinColumns = @JoinColumn(name = "client_id"),
        inverseJoinColumns = @JoinColumn(name = "resource_id")
    )
    private java.util.List<LibraryResource> accessedResources;
}
