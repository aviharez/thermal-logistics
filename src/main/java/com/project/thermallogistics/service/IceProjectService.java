package com.project.thermallogistics.service;

import com.project.thermallogistics.exception.IceProjectNotFoundException;
import com.project.thermallogistics.model.dto.CoolingEquipmentRequest;
import com.project.thermallogistics.model.dto.IceProjectRequest;
import com.project.thermallogistics.model.dto.IceProjectResponse;
import com.project.thermallogistics.model.entity.CoolingEquipment;
import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.model.enums.ProjectStatus;
import com.project.thermallogistics.model.enums.VenueType;
import com.project.thermallogistics.repository.IceProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IceProjectService {

    private final IceProjectRepository repository;

    public IceProjectResponse create(IceProjectRequest request) {
        validateEventTimes(request);

        IceProject project = IceProject.builder()
                .name(request.getName())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .venueName(request.getVenueName())
                .venueType(request.getVenueType())
                .indoorCoolingOffset(resolveIndoorOffset(request))
                .iceType(request.getIceType())
                .sculptureVolumeLiters(request.getSculptureVolumeLiters())
                .eventStartTime(request.getEventStartTime())
                .eventEndTime(request.getEventEndTime())
                .status(ProjectStatus.DRAFT)
                .build();

        if (request.getCoolingEquipments() != null) {
            request.getCoolingEquipments().forEach(eq -> {
                CoolingEquipment equipment = CoolingEquipment.builder()
                        .equipmentType(eq.getEquipmentType())
                        .quantity(eq.getQuantity())
                        .build();
                project.addCoolingEquipment(equipment);
            });
        }

        IceProject saved = repository.save(project);
        log.info("Created ice project '{}' (id={})", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public IceProjectResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<IceProjectResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IceProjectResponse> getByStatus(ProjectStatus status) {
        return repository.findByStatus(status).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public IceProjectResponse update(Long id, IceProjectRequest request) {
        validateEventTimes(request);
        IceProject project = findOrThrow(id);

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setLatitude(request.getLatitude());
        project.setLongitude(request.getLongitude());
        project.setVenueName(request.getVenueName());
        project.setVenueType(request.getVenueType());
        project.setIndoorCoolingOffset(resolveIndoorOffset(request));
        project.setIceType(request.getIceType());
        project.setSculptureVolumeLiters(request.getSculptureVolumeLiters());
        project.setEventStartTime(request.getEventStartTime());
        project.setEventEndTime(request.getEventEndTime());

        project.getCoolingEquipments().clear();
        if (request.getCoolingEquipments() != null) {
            request.getCoolingEquipments().forEach(eq -> {
                CoolingEquipment equipment = CoolingEquipment.builder()
                        .equipmentType(eq.getEquipmentType())
                        .quantity(eq.getQuantity())
                        .build();
                project.addCoolingEquipment(equipment);
            });
        }

        IceProject saved = repository.save(project);
        log.info("Updated ice project '{}' (id={})", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    public IceProjectResponse updateStatus(Long id, ProjectStatus newStatus) {
        IceProject project = findOrThrow(id);
        project.setStatus(newStatus);
        IceProject saved = repository.save(project);
        log.info("Project '{}' status changed to {}", saved.getName(), newStatus);
        return toResponse(saved);
    }

    public IceProjectResponse addEquipment(Long projectId, CoolingEquipmentRequest request) {
        IceProject project = findOrThrow(projectId);
        CoolingEquipment equipment = CoolingEquipment.builder()
                .equipmentType(request.getEquipmentType())
                .quantity(request.getQuantity())
                .build();
        project.addCoolingEquipment(equipment);
        return toResponse(repository.save(project));
    }

    public IceProjectResponse removeEquipment(Long projectId, Long equipmentId) {
        IceProject project = findOrThrow(projectId);
        CoolingEquipment toRemove = project.getCoolingEquipments().stream()
                .filter(e -> e.getId().equals(equipmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Equipment id " + equipmentId + " not found on project " + projectId));
        project.removeCoolingEquipment(toRemove);
        return toResponse(repository.save(project));
    }

    public void delete(Long id) {
        IceProject project = findOrThrow(id);
        repository.delete(project);
        log.info("Deleted ice project '{}' (id={})", project.getName(), id);
    }

    public IceProject findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new IceProjectNotFoundException(id));
    }

    public void captureBaselineTemperature(Long projectId, double temperature) {
        IceProject project = findOrThrow(projectId);
        project.setBaselineTemperatureCelsius(temperature);
        repository.save(project);
        log.info("Baseline temperature captured for project id={}: {} C", projectId, temperature);
    }

    private void validateEventTimes(IceProjectRequest request) {
        if (request.getEventEndTime() != null && request.getEventStartTime() != null && !request.getEventEndTime().isAfter(request.getEventStartTime())) {
            throw new IllegalArgumentException("Event end time must be after event start time");
        }
    }

    private double resolveIndoorOffset(IceProjectRequest request) {
        if (request.getIndoorCoolingOffset() != null) {
            return request.getIndoorCoolingOffset();
        }
        return request.getVenueType() != null
                ? request.getVenueType().getDefaultCoolingOffsetCelsius()
                : VenueType.OUTDOOR.getDefaultCoolingOffsetCelsius();
    }

    public IceProjectResponse toResponse(IceProject project) {
        double durationHours = Duration.between(project.getEventStartTime(), project.getEventEndTime()).toMinutes() / 60.0;

        List<IceProjectResponse.CoolingEquipmentResponse> equipmentResponses = project.getCoolingEquipments().stream()
                .map(eq -> IceProjectResponse.CoolingEquipmentResponse.builder()
                        .id(eq.getId())
                        .equipmentType(eq.getEquipmentType().name())
                        .equipmentDisplayName(eq.getEquipmentType().getDisplayName())
                        .quantity(eq.getQuantity())
                        .totalTemperatureReductionCelsius(eq.getEquipmentType().getTemperatureReductionCelsius() * eq.getQuantity())
                        .build())
                .toList();

        return IceProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .latitude(project.getLatitude())
                .longitude(project.getLongitude())
                .venueName(project.getVenueName())
                .venueType(project.getVenueType())
                .indoorCoolingOffset(project.getIndoorCoolingOffset())
                .iceType(project.getIceType())
                .sculptureVolumeLiters(project.getSculptureVolumeLiters())
                .eventStartTime(project.getEventStartTime())
                .eventEndTime(project.getEventEndTime())
                .status(project.getStatus())
                .coolingEquipments(equipmentResponses)
                .eventDurationHours(Math.round(durationHours * 100.0) / 100.0)
                .baselineTemperatureCelsius(project.getBaselineTemperatureCelsius())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
