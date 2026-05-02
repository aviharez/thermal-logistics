package com.project.thermallogistics.model.entity;

import com.project.thermallogistics.model.enums.AlertSeverity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "thermal_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThermalAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ice_project_id", nullable = false)
    private IceProject iceProject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private Double baselineTemperatureCelsius;

    @Column(nullable = false)
    private Double triggeredTemperatureCelsius;

    @Column(nullable = false)
    private Double temperatureDeltaCelsius;

    @Column(nullable = false)
    @Builder.Default
    private Boolean acknowledged = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime triggeredAt;

}
