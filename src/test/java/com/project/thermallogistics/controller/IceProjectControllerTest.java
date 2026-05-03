package com.project.thermallogistics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.thermallogistics.model.dto.IceProjectRequest;
import com.project.thermallogistics.model.dto.IceProjectResponse;
import com.project.thermallogistics.model.enums.IceType;
import com.project.thermallogistics.model.enums.ProjectStatus;
import com.project.thermallogistics.model.enums.VenueType;
import com.project.thermallogistics.service.IceProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IceProjectController.class)
@DisplayName("IceProjectController - REST API integration test")
public class IceProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IceProjectService service;

    private ObjectMapper objectMapper;
    private IceProjectResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        sampleResponse = IceProjectResponse.builder()
                .id(1L)
                .name("Test Sculpture")
                .latitude(51.5074)
                .longitude(-0.1278)
                .venueName("The Savoy")
                .venueType(VenueType.INDOOR_COOLED)
                .indoorCoolingOffset(8.0)
                .iceType(IceType.CLEAR)
                .sculptureVolumeLiters(200.0)
                .eventStartTime(LocalDateTime.now().plusDays(5))
                .eventEndTime(LocalDateTime.now().plusDays(5).plusHours(4))
                .status(ProjectStatus.DRAFT)
                .coolingEquipments(List.of())
                .eventDurationHours(4.0)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/projects")
    class CreateTests {

        @Test
        @DisplayName("Returns 201 Created with valid payload")
        void createReturns201() throws Exception {
            when(service.create(any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Test Sculpture"))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("Returns 400 when required fields are missing")
        void createReturns400WhenInvalid() throws Exception {
            IceProjectRequest empty = new IceProjectRequest();

            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(empty)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").isMap());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/projects")
    class ReadTests {

        @Test
        @DisplayName("Returns 200 with list of projects")
        void getAllReturns200() throws Exception {
            when(service.getAll()).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/v1/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Test Sculpture"));
        }

        @Test
        @DisplayName("Returns 200 with filtered projects when status param provided")
        void getAllByStatusReturns200() throws Exception {
            when(service.getByStatus(ProjectStatus.DRAFT)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/v1/projects").param("status", "DRAFT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("DRAFT"));
        }

        @Test
        @DisplayName("GET by id returns 200 with project")
        void getByIdReturns200() throws Exception {
            when(service.getById(1L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/projects/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/projects/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Returns 200 with updated project")
        void updateReturns200() throws Exception {
            when(service.update(eq(1L), any())).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/projects/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/projects/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Returns 204 No Content on delete")
        void deleteReturns204() throws Exception {
            mockMvc.perform(delete("/api/v1/projects/1"))
                    .andExpect(status().isNoContent());
        }
    }

    private IceProjectRequest buildValidRequest() {
        IceProjectRequest req = new IceProjectRequest();
        req.setName("Test Sculpture");
        req.setLatitude(51.5074);
        req.setLongitude(-0.1278);
        req.setVenueName("The Savoy");
        req.setVenueType(VenueType.INDOOR_COOLED);
        req.setIndoorCoolingOffset(8.0);
        req.setIceType(IceType.CLEAR);
        req.setSculptureVolumeLiters(200.0);
        req.setEventStartTime(LocalDateTime.now().plusDays(5));
        req.setEventEndTime(LocalDateTime.now().plusDays(5).plusHours(4));
        return req;
    }
}
