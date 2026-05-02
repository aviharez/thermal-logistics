package com.project.thermallogistics.model.dto;

import com.project.thermallogistics.model.enums.EquipmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Cooling equipment unit for an Ice Project")
public class CoolingEquipmentRequest {

    @NotNull(message = "Equipment type is required")
    @Schema(description = "Type of cooling equipment", example = "FAN")
    private EquipmentType equipmentType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 20, message = "Quantity cannot exceed 20 units per type")
    @Schema(description = "Number of units deployed", example = "2")
    private Integer quantity;
}
