package com.project.thermallogistics.model.dto;

import com.project.thermallogistics.model.enums.IceType;
import com.project.thermallogistics.model.enums.VenueType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "Request payload for creating or updating an Ice Project")
public class IceProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 120, message = "Project name must not exceed 120 characters")
    @Schema(description = "Display name for the ice project", example = "Gala Neptune Centerpiece")
    private String name;

    @Size(max = 500)
    @Schema(description = "Additional project notes", example = "VIP table installation for annual charity gala")
    private String description;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(description = "Venue latitude for weather lookup", example = "51.5074")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(description = "Venue longitude for weather lookup", example = "-0.1278")
    private Double longitude;

    @NotBlank(message = "Venue name is required")
    @Size(max = 200)
    @Schema(description = "Name of the event venue", example = "The Savoy Grand Ballroom")
    private String venueName;

    @NotNull(message = "Venue type is required")
    @Schema(description = "Venue climate classification", example = "INDOOR_COOLED")
    private VenueType venueType;

    @NotNull(message = "Indoor cooling offset is required")
    @DecimalMin(value = "0.0", message = "Cooling offset cannot be negative")
    @DecimalMax(value = "20.0", message = "Cooling offset cannot exceed 20 C")
    @Schema(description = "Venue air conditioning temperature reduction in C (overrides venue type default)", example = "8.0")
    private Double indoorCoolingOffset;

    @NotNull(message = "Ice type is required")
    @Schema(description = "Type of ice sculpture material", example = "CLEAR")
    private IceType iceType;

    @NotNull(message = "Sculpture volume is required")
    @DecimalMin(value = "1.0", message = "Volume must be at least 1 liter")
    @DecimalMax(value = "10000.0", message = "Volume cannot exceed 10,000 liters")
    @Schema(description = "Total sculpture volume in liters", example = "250.0")
    private Double sculptureVolumeLiters;

    @NotNull(message = "Event start time is required")
    @Future(message = "Event start time must be in the future")
    @Schema(description = "Event start date and time (ISO-8601)", example = "2026-08-15T18:00:00")
    private LocalDateTime eventStartTime;

    @NotNull(message = "Event end time is required")
    @Schema(description = "Event end date and time (ISO-8601)", example = "2026-08-15T23:00:00")
    private LocalDateTime eventEndTime;

    @Valid
    @Schema(description = "Cooling equipment inventory for this project")
    private List<CoolingEquipmentRequest> coolingEquipments = new ArrayList<>();
}


