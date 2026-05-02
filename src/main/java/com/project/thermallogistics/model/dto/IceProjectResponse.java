package com.project.thermallogistics.model.dto;

import com.project.thermallogistics.model.enums.IceType;
import com.project.thermallogistics.model.enums.ProjectStatus;
import com.project.thermallogistics.model.enums.VenueType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Complete Ice Project response payload")
public class IceProjectResponse {

    @Schema(description = "Unique project identifier")
    private Long id;

    private String name;
    private String description;
    private Double latitude;
    private Double longitude;
    private String venueName;
    private VenueType venueType;
    private Double indoorCoolingOffset;
    private IceType iceType;
    private Double sculptureVolumeLiters;
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;
    private ProjectStatus status;
    private List<CoolingEquipmentResponse> coolingEquipments;

    @Schema(description = "Event duration in hours")
    private Double eventDurationHours;

    @Schema(description = "Baseline temperature captured on first alert check within the 24h pre-event window (C). Null until first check.")
    private Double baselineTemperatureCelsius;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class CoolingEquipmentResponse {
        private Long id;
        private String equipmentType;
        private String equipmentDisplayName;
        private Integer quantity;
        private Double totalTemperatureReductionCelsius;
    }
}
