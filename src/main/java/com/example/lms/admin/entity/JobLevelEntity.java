package com.example.lms.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_levels", uniqueConstraints = {
        @UniqueConstraint(name = "uq_job_levels_group_name", columnNames = {"job_group_id", "name"})
})
public class JobLevelEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_group_id", nullable = false)
    private JobGroupEntity jobGroup;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public JobGroupEntity getJobGroup() { return jobGroup; }
    public void setJobGroup(JobGroupEntity jobGroup) { this.jobGroup = jobGroup; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
