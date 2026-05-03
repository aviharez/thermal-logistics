package com.project.thermallogistics.service;

import com.project.thermallogistics.exception.IceProjectNotFoundException;
import com.project.thermallogistics.model.dto.IceProjectRequest;
import com.project.thermallogistics.model.dto.IceProjectResponse;
import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.model.enums.IceType;
import com.project.thermallogistics.model.enums.ProjectStatus;
import com.project.thermallogistics.model.enums.VenueType;
import com.project.thermallogistics.repository.IceProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("IceProjectService - project lifecycle management")
public class IceProjectServiceTest {

    @Mock private IceProjectRepository repository;

    @InjectMocks
    private IceProjectService service;

    private IceProject sampleProject;
    private IceProjectRequest validRequest;

    @BeforeEach
    void setUp() {
        sampleProject = IceProject.builder()
                .id(1L)
                .name("Sample Sculpture")
                .latitude(51.5074)
                .longitude(-0.1278)
                .venueName("Test Venue")
                .venueType(VenueType.INDOOR_COOLED)
                .indoorCoolingOffset(8.0)
                .iceType(IceType.CLEAR)
                .sculptureVolumeLiters(200.0)
                .eventStartTime(LocalDateTime.now().plusDays(5))
                .eventEndTime(LocalDateTime.now().plusDays(5).plusHours(5))
                .status(ProjectStatus.DRAFT)
                .build();

        validRequest = new IceProjectRequest();
        validRequest.setName("New Sculpture");
        validRequest.setLatitude(51.5074);
        validRequest.setLongitude(-0.1278);
        validRequest.setVenueName("Grand Hall");
        validRequest.setVenueType(VenueType.OUTDOOR);
        validRequest.setIndoorCoolingOffset(0.0);
        validRequest.setIceType(IceType.WHITE);
        validRequest.setSculptureVolumeLiters(150.0);
        validRequest.setEventStartTime(LocalDateTime.now().plusDays(7));
        validRequest.setEventEndTime(LocalDateTime.now().plusDays(7).plusHours(4));
    }

    @Nested
    @DisplayName("Create project")
    class CreateTests {

        @Test
        @DisplayName("Successfully creates a project and returns response with DRAFT status")
        void createProject() {
            when(repository.save(any(IceProject.class))).thenReturn(sampleProject);

            IceProjectResponse response = service.create(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            verify(repository, times(1)).save(any(IceProject.class));
        }

        @Test
        @DisplayName("Throws when event end time is before start time")
        void throwsOnInvalidEventTimes() {
            validRequest.setEventEndTime(validRequest.getEventStartTime().minusHours(1));

            assertThatThrownBy(() -> service.create(validRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("end time must be after");
        }

        @Test
        @DisplayName("Uses venue type default cooling offset when none specified")
        void usesVenueTypeDefaultOffset() {
            validRequest.setVenueType(VenueType.INDOOR_COOLED);
            validRequest.setIndoorCoolingOffset(null);

            IceProject captured = IceProject.builder()
                    .id(2L).name("Test")
                    .latitude(0.0).longitude(0.0).venueName("V")
                    .venueType(VenueType.INDOOR_COOLED)
                    .indoorCoolingOffset(VenueType.INDOOR_COOLED.getDefaultCoolingOffsetCelsius())
                    .iceType(IceType.WHITE).sculptureVolumeLiters(100.0)
                    .eventStartTime(LocalDateTime.now().plusDays(1))
                    .eventEndTime(LocalDateTime.now().plusDays(1).plusHours(3))
                    .build();

            when(repository.save(any())).thenReturn(captured);
            IceProjectResponse resp = service.create(validRequest);

            assertThat(resp.getIndoorCoolingOffset())
                    .isEqualTo(VenueType.INDOOR_COOLED.getDefaultCoolingOffsetCelsius());
        }
    }

    @Nested
    @DisplayName("Read operations")
    class ReadTests {

        @Test
        @DisplayName("getById returns project when found")
        void getByIdFound() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleProject));
            IceProjectResponse response = service.getById(1L);
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Sample Sculpture");
        }

        @Test
        @DisplayName("getById throws IceProjectNotFoundException for unknown id")
        void getByIdNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(IceProjectNotFoundException.class);
        }

        @Test
        @DisplayName("getAll returns all projects")
        void getAll() {
            when(repository.findAll()).thenReturn(List.of(sampleProject));
            List<IceProjectResponse> all = service.getAll();
            assertThat(all).hasSize(1);
        }

        @Test
        @DisplayName("getByStatus filters by the requested status")
        void getByStatus() {
            when(repository.findByStatus(ProjectStatus.DRAFT)).thenReturn(List.of(sampleProject));
            List<IceProjectResponse> drafts = service.getByStatus(ProjectStatus.DRAFT);
            assertThat(drafts).allMatch(p -> p.getStatus() == ProjectStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("Status transitions")
    class StatusTests {

        @Test
        @DisplayName("updateStatus changes project status and saves")
        void updateStatus() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleProject));
            IceProject updated = IceProject.builder()
                    .id(1L).name("Sample Sculpture")
                    .latitude(51.5074).longitude(-0.1278)
                    .venueName("Test Venue").venueType(VenueType.INDOOR_COOLED)
                    .indoorCoolingOffset(8.0).iceType(IceType.CLEAR)
                    .sculptureVolumeLiters(200.0)
                    .eventStartTime(sampleProject.getEventStartTime())
                    .eventEndTime(sampleProject.getEventEndTime())
                    .status(ProjectStatus.SCHEDULED)
                    .build();
            when(repository.save(any())).thenReturn(updated);

            IceProjectResponse response = service.updateStatus(1L, ProjectStatus.SCHEDULED);
            assertThat(response.getStatus()).isEqualTo(ProjectStatus.SCHEDULED);
        }
    }

    @Nested
    @DisplayName("Delete operations")
    class DeleteTests {

        @Test
        @DisplayName("delete removes project when found")
        void deleteFound() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleProject));
            service.delete(1L);
            verify(repository).delete(sampleProject);
        }

        @Test
        @DisplayName("delete throws when project not found")
        void deleteNotFound() {
            when(repository.findById(404L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.delete(404L))
                    .isInstanceOf(IceProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Response mapping")
    class MappingTests {

        @Test
        @DisplayName("toResponse calculates event duration correctly")
        void eventDurationCalculated() {
            IceProject project = IceProject.builder()
                    .id(1L).name("Dur Test").latitude(0.0).longitude(0.0)
                    .venueName("V").venueType(VenueType.OUTDOOR).indoorCoolingOffset(0.0)
                    .iceType(IceType.WHITE).sculptureVolumeLiters(100.0)
                    .eventStartTime(LocalDateTime.of(2026, 8, 1, 18, 0))
                    .eventEndTime(LocalDateTime.of(2026, 8, 1, 22, 30))
                    .status(ProjectStatus.DRAFT)
                    .build();

            IceProjectResponse response = service.toResponse(project);
            assertThat(response.getEventDurationHours()).isEqualTo(4.5);
        }
    }
}
