package com.project.thermallogistics.controller;

import com.project.thermallogistics.model.dto.InstallationSchedule;
import com.project.thermallogistics.service.SchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
@Tag(name = "Installation Schedule", description = "Automated setup and teardown schedule generation")
public class ScheduleController {

    private final SchedulerService schedulerService;

    @GetMapping("/{projectId}")
    @Operation(summary = "Generate installation schedule")
    public ResponseEntity<InstallationSchedule> getSchedule(@PathVariable Long projectId) {
        return ResponseEntity.ok(schedulerService.generateSchedule(projectId));
    }

    @PostMapping("/{projectId}/regenerate")
    @Operation(summary = "Regenerate schedule with latest weather",
            description = "Forces a fresh weather fetch and recalculates the full schedule. " +
                    "Use this endpoint when forecast conditions have changed.")
    public ResponseEntity<InstallationSchedule> regenerateSchedule(@PathVariable Long projectId) {
        return ResponseEntity.ok(schedulerService.generateSchedule(projectId));
    }
}
