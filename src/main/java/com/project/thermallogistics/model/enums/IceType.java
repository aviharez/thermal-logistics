package com.project.thermallogistics.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IceType {

    CLEAR("Clear Ice", 0.85, "High-density, air-free ice. Melts 15% slower than white ice due to reduced surface porosity."),
    WHITE("White Ice", 1.00, "Standard cloudy ice with trapped air bubbles. Baseline melt rate reference.");

    private final String displayName;
    private final double meltRateMultiplier;
    private final String description;
}
