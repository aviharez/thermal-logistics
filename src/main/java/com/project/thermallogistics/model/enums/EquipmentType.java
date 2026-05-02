package com.project.thermallogistics.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EquipmentType {

    FAN("Circulation Fan", 2.5, "Increases evaporative cooling. Reduces effective ambient temperature by 2.5 C per unit."),
    DRY_ICE_BOOSTER("Dry Ice Booster", 7.0, "CO2 sublimation maintains sub-zero microclimate. Reduces effective temperature by 7 C per unit"),
    DRIP_TRAY("Insulated Drip Tray", 0.5, "Collects melt water and provides minor thermal insulation from display surface.");

    private final String displayName;
    private final double temperatureReductionCelsius;
    private final String description;
}
