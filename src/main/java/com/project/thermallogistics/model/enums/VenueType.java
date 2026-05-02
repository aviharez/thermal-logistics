package com.project.thermallogistics.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VenueType {

    OUTDOOR("Outdoor", 0.0, "No shelter from ambient temperature. Full weather exposure."),
    INDOOR_UNCOOLED("Indoor (Uncooled)", 2.0, "Indoor venue without air conditioning. Slight temperature reduction from shade."),
    INDOOR_COOLED("Indoor (Climate Controlled)", 8.0, "Air-conditioned indoor venue. Significant ambient temperature reduction.");

    private final String displayName;
    private final double defaultCoolingOffsetCelsius;
    private final String description;
}
