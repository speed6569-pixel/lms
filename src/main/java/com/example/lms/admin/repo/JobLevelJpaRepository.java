package com.example.lms.admin.repo;

import com.example.lms.admin.entity.JobGroupEntity;
import com.example.lms.admin.entity.JobLevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobLevelJpaRepository extends JpaRepository<JobLevelEntity, Long> {
    Optional<JobLevelEntity> findByJobGroupAndNameIgnoreCase(JobGroupEntity jobGroup, String name);
    List<JobLevelEntity> findByActiveTrueOrderByNameAsc();
    List<JobLevelEntity> findByJobGroupAndActiveTrueOrderByNameAsc(JobGroupEntity jobGroup);
}
