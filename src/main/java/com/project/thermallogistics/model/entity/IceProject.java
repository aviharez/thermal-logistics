package com.project.thermallogistics.model.entity;

import com.project.thermallogistics.model.enums.IceType;
import com.project.thermallogistics.model.enums.ProjectStatus;
import com.project.thermallogistics.model.enums.VenueType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ice_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IceProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, length = 200)
    private String venueName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VenueType venueType;

    @Column(nullable = false)
    private Double indoorCoolingOffset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IceType iceType;

    @Column(nullable = false)
    private Double sculptureVolumeLiters;

    @Column(nullable = false)
    private LocalDateTime eventStartTime;

    @Column(nullable = false)
    private LocalDateTime eventEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.DRAFT;

    @Column
    private Double baselineTemperatureCelsius;

    @OneToMany(mappedBy = "iceProject", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private List<CoolingEquipment> coolingEquipments = new ArrayList<>();

    @OneToMany(mappedBy = "iceProject", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ThermalAlert> alerts = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addCoolingEquipment(CoolingEquipment equipment) {
        equipment.setIceProject(this);
        coolingEquipments.add(equipment);
    }

    public void removeCoolingEquipment(CoolingEquipment equipment) {
        coolingEquipments.remove(equipment);
        equipment.setIceProject(null);
    }
}
