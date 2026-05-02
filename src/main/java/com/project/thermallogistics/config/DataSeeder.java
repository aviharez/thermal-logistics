package com.project.thermallogistics.config;

import com.project.thermallogistics.model.entity.CoolingEquipment;
import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.model.enums.EquipmentType;
import com.project.thermallogistics.model.enums.IceType;
import com.project.thermallogistics.model.enums.ProjectStatus;
import com.project.thermallogistics.model.enums.VenueType;
import com.project.thermallogistics.repository.IceProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final IceProjectRepository repository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (repository.count() > 0) return;

        log.info("Seeding demo ice projects...");

        IceProject london = IceProject.builder()
                .name("Mayfair Gala Neptune Sculpture")
                .description("4-meter Neptune centerpiece for annual charity gala. VIP photography window critical.")
                .latitude(51.5074)
                .longitude(-0.1278)
                .venueName("The Savoy Grand Ballroom")
                .venueType(VenueType.INDOOR_COOLED)
                .indoorCoolingOffset(9.0)
                .iceType(IceType.CLEAR)
                .sculptureVolumeLiters(480.0)
                .eventStartTime(LocalDateTime.now().plusDays(5).withHour(19).withMinute(0))
                .eventEndTime(LocalDateTime.now().plusDays(5).withHour(23).withMinute(30))
                .status(ProjectStatus.SCHEDULED)
                .build();

        CoolingEquipment fan1 = CoolingEquipment.builder().equipmentType(EquipmentType.FAN).quantity(2).build();
        CoolingEquipment dryIce1 = CoolingEquipment.builder().equipmentType(EquipmentType.DRY_ICE_BOOSTER).quantity(3).build();
        CoolingEquipment tray1 = CoolingEquipment.builder().equipmentType(EquipmentType.DRIP_TRAY).quantity(4).build();
        london.addCoolingEquipment(fan1);
        london.addCoolingEquipment(dryIce1);
        london.addCoolingEquipment(tray1);

        IceProject dubai = IceProject.builder()
                .name("Dubai Marina Seafood Buffet Display")
                .description("12-piece chilled seafood display on open-air marina terrace. High-heat challenge.")
                .latitude(25.2048)
                .longitude(55.2708)
                .venueName("One&Only Royal Mirage - Arabian Court Terrace")
                .venueType(VenueType.OUTDOOR)
                .indoorCoolingOffset(0.0)
                .iceType(IceType.WHITE)
                .sculptureVolumeLiters(120.0)
                .eventStartTime(LocalDateTime.now().plusDays(3).withHour(20).withMinute(0))
                .eventEndTime(LocalDateTime.now().plusDays(3).withHour(23).withMinute(0))
                .status(ProjectStatus.DRAFT)
                .build();

        CoolingEquipment fan2 = CoolingEquipment.builder().equipmentType(EquipmentType.FAN).quantity(4).build();
        CoolingEquipment dryIce2 = CoolingEquipment.builder().equipmentType(EquipmentType.DRY_ICE_BOOSTER).quantity(5).build();
        dubai.addCoolingEquipment(fan2);
        dubai.addCoolingEquipment(dryIce2);

        IceProject newYork = IceProject.builder()
                .name("Manhattan Rooftop Swan Pair")
                .description("Twin swan sculptures for rooftop wedding cocktail hour.")
                .latitude(40.7128)
                .longitude(-74.0060)
                .venueName("The Rainbow Room, Rock Center Rooftop")
                .venueType(VenueType.INDOOR_UNCOOLED)
                .indoorCoolingOffset(3.0)
                .iceType(IceType.CLEAR)
                .sculptureVolumeLiters(85.0)
                .eventStartTime(LocalDateTime.now().plusDays(10).withHour(17).withMinute(0))
                .eventEndTime(LocalDateTime.now().plusDays(10).withHour(20).withMinute(0))
                .status(ProjectStatus.DRAFT)
                .build();

        CoolingEquipment fan3 = CoolingEquipment.builder().equipmentType(EquipmentType.FAN).quantity(1).build();
        CoolingEquipment tray3 = CoolingEquipment.builder().equipmentType(EquipmentType.DRIP_TRAY).quantity(2).build();
        newYork.addCoolingEquipment(fan3);
        newYork.addCoolingEquipment(tray3);

        repository.save(london);
        repository.save(dubai);
        repository.save(newYork);

        log.info("Seeded {} demo ice projects.", repository.count());
    }
}
