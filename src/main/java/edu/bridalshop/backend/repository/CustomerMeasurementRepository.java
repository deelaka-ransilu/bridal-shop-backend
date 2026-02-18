package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.CustomerMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerMeasurementRepository extends JpaRepository<CustomerMeasurement, Integer> {

    // Get latest active measurement for a customer
    Optional<CustomerMeasurement> findByCustomerUserIdAndIsActiveTrue(Integer customerId);

    // Check if customer has any measurement
    boolean existsByCustomerUserId(Integer customerId);

    // Deactivate all active measurements for a customer before inserting new one
    @Modifying
    @Query("""
        UPDATE CustomerMeasurement m
        SET m.isActive = false
        WHERE m.customer.userId = :customerId
        AND m.isActive = true
        """)
    void deactivateAllForCustomer(@Param("customerId") Integer customerId);

    // Check if employee has an assigned order for a specific customer
    // Used for access control: employee can only view customer they are assigned to
    @Query("""
        SELECT COUNT(o) > 0
        FROM Order o
        WHERE o.handledByEmployee.employeeId = :employeeId
        AND o.customer.userId = :customerId
        AND o.orderStatus NOT IN ('CANCELLED')
        """)
    boolean isEmployeeAssignedToCustomer(
            @Param("employeeId") Integer employeeId,
            @Param("customerId") Integer customerId
    );
}
