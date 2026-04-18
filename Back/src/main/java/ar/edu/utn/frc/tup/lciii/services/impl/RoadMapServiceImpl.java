package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.dtos.CreateRoadmapRequest;
import ar.edu.utn.frc.tup.lciii.dtos.GeneratorDto;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapDetailDto;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapDto;
import ar.edu.utn.frc.tup.lciii.dtos.WaypointDto;
import ar.edu.utn.frc.tup.lciii.entities.EmployeesEntity;
import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.OrdersEntity;
import ar.edu.utn.frc.tup.lciii.entities.RoadmapDetailEntity;
import ar.edu.utn.frc.tup.lciii.entities.RoadmapEntity;
import ar.edu.utn.frc.tup.lciii.entities.SealEntity;
import ar.edu.utn.frc.tup.lciii.models.Roadmap;
import ar.edu.utn.frc.tup.lciii.models.RoadmapStatus;
import ar.edu.utn.frc.tup.lciii.models.SealStatus;
import ar.edu.utn.frc.tup.lciii.repositories.EmployeeRepository;
import ar.edu.utn.frc.tup.lciii.repositories.OrdersRepository;
import ar.edu.utn.frc.tup.lciii.repositories.RoadmapDetailRepository;
import ar.edu.utn.frc.tup.lciii.repositories.RoadmapRepository;
import ar.edu.utn.frc.tup.lciii.repositories.SealRepository;
import ar.edu.utn.frc.tup.lciii.services.RoadMapService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class RoadMapServiceImpl implements RoadMapService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private RoadmapRepository repository;
    @Autowired
    private OrdersRepository ordersRepository;
    @Autowired
    private RoadmapDetailRepository roadmapDetailRepository;
    @Autowired
    private SealRepository sealRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public RoadmapDto createRoadmapFromOrders(CreateRoadmapRequest request) {
        // 1. Validar la solicitud
        validateCreateRequest(request);

        // 2. Obtener los pedidos seleccionados
        List<OrdersEntity> selectedOrders = ordersRepository.findAllById(request.getSelectedOrderIds());

        if (selectedOrders.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron pedidos válidos");
        }

        // 3. Verificar que todos los pedidos estén en estado PENDIENTE
        for (OrdersEntity order : selectedOrders) {
            if (!"PENDIENTE".equalsIgnoreCase(order.getState())) {
                throw new IllegalArgumentException("El pedido con ID " + order.getId() + " no está en estado PENDIENTE (estado actual: " + order.getState() + ")");
            }
        }

        // 4. Crear la hoja de ruta principal
        RoadmapEntity roadmap = new RoadmapEntity();
        roadmap.setDate(LocalDateTime.now());
        roadmap.setZone(request.getZone());
        roadmap.setCollectDate(parseDateTime(request.getCollectDate()));
        roadmap.setEmployee(request.getEmployee());
        roadmap.setExitHour(parseTime(request.getExitHour()));
        roadmap.setStatus(RoadmapStatus.EN_PROGRESO);

        // 5. Guardar la hoja de ruta primero
        roadmap = repository.save(roadmap);
        System.out.println("✅ Hoja de ruta creada con ID: " + roadmap.getRoadmapId());

        // 6. Crear los detalles para cada pedido
        List<RoadmapDetailEntity> details = new ArrayList<>();

        for (OrdersEntity order : selectedOrders) {
            RoadmapDetailEntity detail = new RoadmapDetailEntity();
            detail.setRoadmapEntity(roadmap);
            detail.setGeneratorEntity(order.getGeneratorEntity());
            details.add(detail);

            // 7. Actualizar el estado del pedido
            order.setState("EN_PROCESO");
            order.setScheduledDate(roadmap.getCollectDate());

            System.out.println("✅ Detalle creado para generador: " + order.getGeneratorEntity().getName());
        }

        // 8. Guardar detalles y pedidos actualizados
        roadmapDetailRepository.saveAll(details);
        ordersRepository.saveAll(selectedOrders);

        System.out.println("✅ " + details.size() + " detalles guardados");
        System.out.println("✅ " + selectedOrders.size() + " pedidos actualizados a EN_PROCESO");

        // 9. Actualizar la roadmap con los detalles
        roadmap.setDetails(details);

        // 10. Convertir a DTO y retornar
        return mapEntityToDto(roadmap);
    }


    private void validateCreateRequest(CreateRoadmapRequest request) {
        if (request.getZone() == null || request.getZone().trim().isEmpty()) {
            throw new IllegalArgumentException("La zona es requerida");
        }
        if (request.getEmployee() == null || request.getEmployee().trim().isEmpty()) {
            throw new IllegalArgumentException("El empleado es requerido");
        }
        if (request.getCollectDate() == null || request.getCollectDate().trim().isEmpty()) {
            throw new IllegalArgumentException("La fecha de recolección es requerida");
        }
        if (request.getExitHour() == null || request.getExitHour().trim().isEmpty()) {
            throw new IllegalArgumentException("La hora de salida es requerida");
        }
        if (request.getSelectedOrderIds() == null || request.getSelectedOrderIds().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un pedido");
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // Primero intentar con el formato ISO con zona horaria (Z)
            if (dateTimeStr.endsWith("Z")) {
                // Parsear como ZonedDateTime y convertir a LocalDateTime
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                return zonedDateTime.toLocalDateTime();
            }

            // Si no tiene Z, intentar con formato ISO local
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        } catch (Exception e) {
            // Como fallback, intentar otros formatos comunes
            try {
                // Intentar formato ISO con offset (+00:00, -03:00, etc.)
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeStr);
                return offsetDateTime.toLocalDateTime();
            } catch (Exception e2) {
                throw new IllegalArgumentException("Formato de fecha inválido: " + dateTimeStr +
                        ". Formatos soportados: ISO_ZONED_DATE_TIME, ISO_LOCAL_DATE_TIME, ISO_OFFSET_DATE_TIME");
            }
        }
    }

    private LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de hora inválido: " + timeStr);
        }
    }
    private RoadmapDto mapEntityToDto(RoadmapEntity entity) {
        RoadmapDto dto = new RoadmapDto();

        dto.setRoadmapId(entity.getRoadmapId());
        dto.setDate(entity.getDate());
        dto.setZone(entity.getZone());
        dto.setCollectDate(entity.getCollectDate());
        dto.setEmployee(entity.getEmployee());
        dto.setExitHour(entity.getExitHour());
        dto.setReturnHour(entity.getReturnHour());
        dto.setCollectHour(entity.getCollectHour());
        dto.setInitialKm(entity.getInitialKm());
        dto.setFinalKm(entity.getFinalKm());
        dto.setCountBags(entity.getCountBags());

        // Mapear detalles básicos
        if (entity.getDetails() != null && !entity.getDetails().isEmpty()) {
            List<RoadmapDetailDto> detailDtos = entity.getDetails().stream()
                    .map(this::mapDetailEntityToDto)
                    .collect(Collectors.toList());
            dto.setDetails(detailDtos);
        }

        dto.setState("PENDIENTE");

        return dto;
    }

    private RoadmapDetailDto mapDetailEntityToDto(RoadmapDetailEntity entity) {
        RoadmapDetailDto dto = new RoadmapDetailDto();
        dto.setId(entity.getId());

        if (entity.getGeneratorEntity() != null) {
            GeneratorEntity generator = entity.getGeneratorEntity();
            dto.setGeneratorId(generator.getGeneratorId());
            dto.setGeneratorName(generator.getName());
            dto.setGeneratorAddress(generator.getAddress());
            dto.setLatitude(generator.getLatitude());
            dto.setLongitude(generator.getLongitude());
            dto.setGeneratorType(generator.getType());
            dto.setGeneratorContact(generator.getContact());
            dto.setGeneratorEmail(generator.getEmail());

        }

        return dto;
    }

    @Override
    public List<RoadmapDto> getAllRoadmaps() {
        List<RoadmapEntity> roadmaps = repository.findAllWithDetails();
        return roadmaps.stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public RoadmapDto getRoadmapById(Long id) {
        RoadmapEntity roadmap = repository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Hoja de ruta no encontrada: " + id));
        return mapEntityToDto(roadmap);
    }

    @Override
    public Roadmap createRoadMap(RoadmapDto roadMapDto) {
        RoadmapEntity roadmapEntity = new RoadmapEntity();
        roadmapEntity.setZone(roadMapDto.getZone());
        roadmapEntity.setDate(LocalDateTime.now());
        roadmapEntity.setCollectHour(roadMapDto.getCollectHour());
        roadmapEntity.setEmployee(roadMapDto.getEmployee());

        List<RoadmapDetailEntity> detailEntities = new ArrayList<>();

        for (RoadmapDetailDto detailDto : roadMapDto.getDetails()) {
            RoadmapDetailEntity detailEntity = new RoadmapDetailEntity();
            detailEntity.setRoadmapEntity(roadmapEntity);

            GeneratorDto generatorDto = detailDto.getGenerator();
            GeneratorEntity generatorEntity = modelMapper.map(generatorDto, GeneratorEntity.class);

            detailEntity.setGeneratorEntity(generatorEntity);
            detailEntities.add(detailEntity);
        }

        roadmapEntity.setDetails(detailEntities);
        RoadmapEntity saved = repository.save(roadmapEntity);
        return modelMapper.map(saved, Roadmap.class);
    }

    @Override
    public List<WaypointDto> calculateOptimizedRoute(Long roadmapId) {
        return optimizeRoute(roadmapId);
    }

    // ✅ MANTENER - Métodos de optimización de rutas
    public List<WaypointDto> optimizeRoute(Long roadmapId) {
        RoadmapEntity roadmap = repository.findById(roadmapId)
                .orElseThrow(() -> new EntityNotFoundException("Roadmap not found"));

        List<GeneratorEntity> generators = roadmap.getDetails().stream()
                .map(RoadmapDetailEntity::getGeneratorEntity)
                .filter(g -> g.getLatitude() != null && g.getLongitude() != null)
                .toList();

        if (generators.isEmpty()) {
            return Collections.emptyList();
        }

        GeneratorEntity start = generators.get(0);
        List<GeneratorEntity> unvisited = new ArrayList<>(generators);
        List<WaypointDto> optimized = new ArrayList<>();

        GeneratorEntity current = start;
        unvisited.remove(current);
        optimized.add(new WaypointDto(current.getLatitude(), current.getLongitude()));

        while (!unvisited.isEmpty()) {
            GeneratorEntity next = findNearest(current, unvisited);
            optimized.add(new WaypointDto(next.getLatitude(), next.getLongitude()));
            unvisited.remove(next);
            current = next;
        }

        return optimized;
    }

    private GeneratorEntity findNearest(GeneratorEntity current, List<GeneratorEntity> candidates) {
        return candidates.stream()
                .min(Comparator.comparingDouble(g -> distance(
                        current.getLatitude(), current.getLongitude(),
                        g.getLatitude(), g.getLongitude())))
                .orElseThrow(() -> new IllegalStateException("No se pudo encontrar el generador más cercano"));
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    @Transactional
    public void checkAndCompleteRoadmap(Long roadmapId) {
        RoadmapEntity roadmap = repository.findById(roadmapId)
                .orElseThrow(() -> new EntityNotFoundException("Hoja de ruta no encontrada"));

        // Solo verificar si está EN_PROGRESO
        if (roadmap.getStatus() != RoadmapStatus.EN_PROGRESO) {
            return; // No hacer nada si no está en progreso
        }

        // Verificar progreso de recolección
        int totalSeals = 0;
        int collectedSeals = 0;

        for (RoadmapDetailEntity detail : roadmap.getDetails()) {
            GeneratorEntity generator = detail.getGeneratorEntity();

            // Contar precintos del generador
            List<SealEntity> generatorSeals = sealRepository.findByGeneratorEntityAndStateIn(
                    generator, Arrays.asList(SealStatus.OCUPADO, SealStatus.RECOLECTADO));

            totalSeals += generatorSeals.size();

            long collected = generatorSeals.stream()
                    .filter(s -> s.getState() == SealStatus.RECOLECTADO)
                    .count();

            collectedSeals += collected;
        }

        // Completar automáticamente si se recolectó 90% o más
        double completionPercentage = totalSeals > 0 ? (double) collectedSeals / totalSeals : 0;

        if (completionPercentage >= 0.9) { // 90% o más
            roadmap.setStatus(RoadmapStatus.COMPLETADA);
            repository.save(roadmap);

            log.info("✅ Hoja de ruta {} completada automáticamente - {}% completado ({}/{})",
                    roadmapId, Math.round(completionPercentage * 100), collectedSeals, totalSeals);
        }
    }

    // 🆕 MÉTODO MANUAL: Completar ruta manualmente
    @Override
    @Transactional
    public void completeRoadmapManually(Long roadmapId, Long employeeId) {
        RoadmapEntity roadmap = repository.findById(roadmapId)
                .orElseThrow(() -> new EntityNotFoundException("Hoja de ruta no encontrada"));

        // Verificar que pertenece al empleado
        EmployeesEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado"));

        if (!roadmap.getEmployee().equals(employee.getName())) {
            throw new IllegalStateException("Esta hoja de ruta no te pertenece");
        }

        // Verificar que esté en progreso
        if (roadmap.getStatus() != RoadmapStatus.EN_PROGRESO) {
            throw new IllegalStateException("Solo se pueden completar rutas en progreso. Estado actual: " + roadmap.getStatus());
        }

        // Verificar que tenga al menos algunos precintos recolectados
        Long collectedCount = repository.countCollectedSealsByRoadmap(roadmapId);
        if (collectedCount == 0) {
            throw new IllegalStateException("No se puede completar una ruta sin precintos recolectados");
        }

        // Completar la ruta
        roadmap.setStatus(RoadmapStatus.COMPLETADA);
        repository.save(roadmap);

        log.info("✅ Hoja de ruta {} completada manualmente por empleado {} - {} precintos recolectados",
                roadmapId, employee.getName(), collectedCount);
    }
}

