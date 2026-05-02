package com.project.thermallogistics.repository;


import com.project.thermallogistics.model.entity.ThermalAlert;
import com.project.thermallogistics.model.enums.AlertSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ThermalAlertRepository extends JpaRepository<ThermalAlert, Long> {

    List<ThermalAlert> findByIceProjectId(Long projectId);

    List<ThermalAlert> findByIceProjectIdAndAcknowledgedFalse(Long projectId);

    List<ThermalAlert> findBySeverityIn(List<AlertSeverity> severities);

    List<ThermalAlert> findByAcknowledgedFalse();

    @Query("SELECT a FROM ThermalAlert a WHERE a.iceProject.id = :projectId AND a.triggeredAt >= :since ORDER BY a.triggeredAt DESC")
    List<ThermalAlert> findRecentAlertsForProject(@Param("projectId") Long projectId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM ThermalAlert a WHERE a.iceProject.id = :projectId AND a.acknowledged = false")
    long countUnacknowledgedByProjectId(@Param("projectId") Long projectId);
}
