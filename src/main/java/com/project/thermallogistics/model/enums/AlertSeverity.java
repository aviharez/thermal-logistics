package com.project.thermallogistics.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertSeverity {

    LOW("Temperature increase of 1-3 C above baseline forecast"),
    MEDIUM("Temperature increase of 3-5 C. Melt rate elevated; monitor closely."),
    HIGH("Temperature increase of 5-8 C. Significant reduction in safe display window."),
    CRITICAL("Temperature increase exceeds 8 C or structural failure window breached.");

    private final String description;
}
