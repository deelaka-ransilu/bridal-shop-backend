package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    Optional<Employee> findByUser_UserId(Integer userId);

    boolean existsByUser_UserId(Integer userId);
}
