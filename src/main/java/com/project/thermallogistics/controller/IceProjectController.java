package com.project.thermallogistics.controller;

import com.project.thermallogistics.model.dto.CoolingEquipmentRequest;
import com.project.thermallogistics.model.dto.IceProjectRequest;
import com.project.thermallogistics.model.dto.IceProjectResponse;
import com.project.thermallogistics.model.enums.ProjectStatus;
import com.project.thermallogistics.service.IceProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Ice Projects", description = "CRUS operations for Ice Sculpture Projects")
public class IceProjectController {

    private final IceProjectService service;

    @PostMapping
    @Operation(summary = "Create a new Ice Project",
            description = "Creates a new ice project with sculpture metadata, venue details, and cooling equipment.")
    public ResponseEntity<IceProjectResponse> create(@Valid @RequestBody IceProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @Operation(summary = "List all Ice Projects",
            description = "Returns all projects. Optionally filter by status using the `status` query parameter.")
    public ResponseEntity<List<IceProjectResponse>> getAll(@Parameter(description = "Filter by project status") @RequestParam(required = false)ProjectStatus status) {
        List<IceProjectResponse> projects = status != null ? service.getByStatus(status) : service.getAll();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Ice Project by ID")
    public ResponseEntity<IceProjectResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an Ice Project",
            description = "Full replacement update. All fields including cooling equipment list will be overwritten.")
    public ResponseEntity<IceProjectResponse> update(@PathVariable Long id, @Valid @RequestBody IceProjectRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update project status",
            description = "Transitions the project through its lifecycle: DRAFT -> SCHEDULED -> ACTIVE -> COMPLETED.")
    public ResponseEntity<IceProjectResponse> updateStatus(
            @PathVariable Long id,
            @Parameter(description = "New project status", required = true)
            @RequestParam ProjectStatus status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an Ice Project")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/equipment")
    @Operation(summary = "Add cooling equipment to a project")
    public ResponseEntity<IceProjectResponse> addEquipment(
            @PathVariable Long id,
            @Valid @RequestBody CoolingEquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addEquipment(id, request));
    }

    @DeleteMapping("/{id}/equipment/{equipmentId}")
    @Operation(summary = "Remove cooling equipment from a project")
    public ResponseEntity<IceProjectResponse> removeEquipment(
            @PathVariable Long id,
            @PathVariable Long equipmentId) {
        return ResponseEntity.ok(service.removeEquipment(id, equipmentId));
    }
}
