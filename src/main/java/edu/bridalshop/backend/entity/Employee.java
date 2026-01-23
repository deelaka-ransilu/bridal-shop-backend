package edu.bridalshop.backend.entity;

import edu.bridalshop.backend.enums.EmploymentType;
import edu.bridalshop.backend.enums.SalaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    @Builder.Default
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", nullable = false)
    @Builder.Default
    private SalaryType salaryType = SalaryType.MONTHLY;

    @Column(name = "base_salary", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Column(name = "hire_date")
    @Builder.Default
    private LocalDate hireDate = LocalDate.now();

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
