package com.kaksha.library.repository;

import com.kaksha.library.model.entity.Payments;
import com.kaksha.library.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentsRepository extends JpaRepository<Payments, Long> {
    
    Optional<Payments> findByTransactionReference(String transactionReference);
    
    List<Payments> findByClientUserID(Long clientId);
    
    Page<Payments> findByClientUserIDOrderByCreatedAtDesc(Long clientId, Pageable pageable);
    
    List<Payments> findByStatus(PaymentStatus status);
    
    List<Payments> findByResourceResourceID(Long resourceId);
    
    @Query("SELECT p FROM Payments p WHERE p.client.userID = :clientId AND p.resource.resourceID = :resourceId AND p.status = 'PAID'")
    Optional<Payments> findSuccessfulPurchaseByClientAndResource(@Param("clientId") Long clientId, @Param("resourceId") Long resourceId);
    
    @Query("SELECT COUNT(p) FROM Payments p WHERE p.status = 'PAID'")
    long countSuccessfulPayments();
    
    @Query("SELECT SUM(p.amount) FROM Payments p WHERE p.status = 'PAID'")
    BigDecimal sumTotalRevenue();
    
    @Query("SELECT SUM(p.amount) FROM Payments p WHERE p.status = 'PAID' AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM Payments p WHERE p.status = 'PAID' ORDER BY p.createdAt DESC")
    List<Payments> findRecentSuccessfulPayments(Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Payments p WHERE p.status = 'PAID' AND p.createdAt BETWEEN :startDate AND :endDate")
    long countPaymentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(p) FROM Payments p WHERE p.status = 'PAID' AND p.client.userID = :clientId")
    long countByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT DATE(p.paidAt) as date, COUNT(p) as purchases, SUM(p.amount) as revenue " +
           "FROM Payments p WHERE p.status = 'PAID' AND p.paidAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(p.paidAt) ORDER BY DATE(p.paidAt)")
    List<Object[]> getDailyRevenueStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p.resource.category as category, COUNT(p) as count " +
           "FROM Payments p WHERE p.status = 'PAID' AND p.client.userID = :clientId " +
           "GROUP BY p.resource.category")
    List<Object[]> getClientPurchasesByCategory(@Param("clientId") Long clientId);
    
    // Find best selling resources (most purchased)
    @Query("SELECT p.resource.resourceID, COUNT(p) as salesCount " +
           "FROM Payments p WHERE p.status = 'PAID' " +
           "GROUP BY p.resource.resourceID " +
           "ORDER BY salesCount DESC")
    List<Object[]> findBestSellingResources(Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Payments p WHERE p.status = 'PAID' AND p.resource.resourceID = :resourceId")
    long countSalesByResourceId(@Param("resourceId") Long resourceId);
}
