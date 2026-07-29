package com.example1.getyourride.repository;
import com.example1.getyourride.entity.DriverApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DriverApplicationRepository extends JpaRepository<DriverApplication, Long> {
    Optional<DriverApplication> findByStudentId(Long studentId);
}
