package com.project.thermallogistics.repository;

import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.model.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IceProjectRepository extends JpaRepository<IceProject, Long> {

    List<IceProject> findByStatus(ProjectStatus status);

    List<IceProject> findByEventStartTimeBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT p FROM IceProject p WHERE p.eventStartTime BETWEEN :from AND :to AND p.status NOT IN ('CANCELLED', 'COMPLETED')")
    List<IceProject> findActiveProjectsInWindow(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT p FROM IceProject p WHERE p.status = 'ACTIVE' OR p.status = 'SCHEDULED'")
    List<IceProject> findAllLiveProjects();

    boolean existsByNameAndEventStartTime(String name, LocalDateTime eventStartTime);
}
