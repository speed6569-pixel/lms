package com.example.lms.admin.repo;

import com.example.lms.admin.entity.JobGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobGroupJpaRepository extends JpaRepository<JobGroupEntity, Long> {
    Optional<JobGroupEntity> findByNameIgnoreCase(String name);
    List<JobGroupEntity> findByActiveTrueOrderByNameAsc();
}
