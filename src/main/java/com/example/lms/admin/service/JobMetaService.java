package com.example.lms.admin.service;

import com.example.lms.admin.entity.JobGroupEntity;
import com.example.lms.admin.entity.JobLevelEntity;
import com.example.lms.admin.repo.JobGroupJpaRepository;
import com.example.lms.admin.repo.JobLevelJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class JobMetaService {
    private final JobGroupJpaRepository jobGroupJpaRepository;
    private final JobLevelJpaRepository jobLevelJpaRepository;

    public JobMetaService(JobGroupJpaRepository jobGroupJpaRepository,
                          JobLevelJpaRepository jobLevelJpaRepository) {
        this.jobGroupJpaRepository = jobGroupJpaRepository;
        this.jobLevelJpaRepository = jobLevelJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<String> getActiveGroups() {
        return jobGroupJpaRepository.findByActiveTrueOrderByNameAsc().stream().map(JobGroupEntity::getName).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getActiveLevelsMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (JobGroupEntity g : jobGroupJpaRepository.findByActiveTrueOrderByNameAsc()) {
            List<String> levels = jobLevelJpaRepository.findByJobGroupAndActiveTrueOrderByNameAsc(g)
                    .stream().map(JobLevelEntity::getName).toList();
            map.put(g.getName(), levels);
        }
        return map;
    }

    @Transactional
    public JobGroupEntity addGroup(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("직군명을 입력해 주세요.");
        return jobGroupJpaRepository.findByNameIgnoreCase(name.trim())
                .map(existing -> {
                    existing.setActive(true);
                    return jobGroupJpaRepository.save(existing);
                })
                .orElseGet(() -> {
                    JobGroupEntity g = new JobGroupEntity();
                    g.setName(name.trim());
                    g.setActive(true);
                    return jobGroupJpaRepository.save(g);
                });
    }

    @Transactional
    public JobLevelEntity addLevel(String groupName, String levelName) {
        if (groupName == null || groupName.isBlank()) throw new IllegalArgumentException("직군을 선택해 주세요.");
        if (levelName == null || levelName.isBlank()) throw new IllegalArgumentException("직급명을 입력해 주세요.");

        JobGroupEntity group = jobGroupJpaRepository.findByNameIgnoreCase(groupName.trim())
                .orElseGet(() -> addGroup(groupName.trim()));
        group.setActive(true);
        jobGroupJpaRepository.save(group);

        return jobLevelJpaRepository.findByJobGroupAndNameIgnoreCase(group, levelName.trim())
                .map(existing -> {
                    existing.setActive(true);
                    return jobLevelJpaRepository.save(existing);
                })
                .orElseGet(() -> {
                    JobLevelEntity l = new JobLevelEntity();
                    l.setJobGroup(group);
                    l.setName(levelName.trim());
                    l.setActive(true);
                    return jobLevelJpaRepository.save(l);
                });
    }

    @Transactional
    public void deactivateGroup(Long id) {
        JobGroupEntity g = jobGroupJpaRepository.findById(id).orElseThrow();
        g.setActive(false);
        jobGroupJpaRepository.save(g);
    }

    @Transactional
    public void deactivateLevel(Long id) {
        JobLevelEntity l = jobLevelJpaRepository.findById(id).orElseThrow();
        l.setActive(false);
        jobLevelJpaRepository.save(l);
    }
}
