package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.dtos.EmployeeRoadmapResponse;
import ar.edu.utn.frc.tup.lciii.dtos.GeneratorInRoadmap;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapCloseRequest;
import ar.edu.utn.frc.tup.lciii.dtos.RoadmapCloseResponse;
import ar.edu.utn.frc.tup.lciii.dtos.ScanQRRequest;
import ar.edu.utn.frc.tup.lciii.dtos.ScanQRResponse;
import ar.edu.utn.frc.tup.lciii.dtos.SealInfo;
import ar.edu.utn.frc.tup.lciii.dtos.UserDTO;
import ar.edu.utn.frc.tup.lciii.entities.EmployeesEntity;
import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.OrdersEntity;
import ar.edu.utn.frc.tup.lciii.entities.RoadmapDetailEntity;
import ar.edu.utn.frc.tup.lciii.entities.RoadmapEntity;
import ar.edu.utn.frc.tup.lciii.entities.SealEntity;
import ar.edu.utn.frc.tup.lciii.entities.UserEntity;
import ar.edu.utn.frc.tup.lciii.models.Employee;
import ar.edu.utn.frc.tup.lciii.models.RoadmapStatus;
import ar.edu.utn.frc.tup.lciii.models.Role;
import ar.edu.utn.frc.tup.lciii.models.SealStatus;
import ar.edu.utn.frc.tup.lciii.repositories.EmployeeRepository;
import ar.edu.utn.frc.tup.lciii.repositories.OrdersRepository;
import ar.edu.utn.frc.tup.lciii.repositories.RoadmapRepository;
import ar.edu.utn.frc.tup.lciii.repositories.SealRepository;
import ar.edu.utn.frc.tup.lciii.services.EmployeeService;
import ar.edu.utn.frc.tup.lciii.services.EnvioCorreosService;
import ar.edu.utn.frc.tup.lciii.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private EnvioCorreosService envioCorreosService;
    @Autowired
    private RoadmapRepository roadmapRepository;
    @Autowired
    private SealRepository sealRepository;
    @Autowired
    private OrdersRepository ordersRepository;
    @Override
    @Transactional
    public Employee registerEmployee(Employee employee, String email) {
        if(employeeRepository.existsByName(employee.getName())){
            throw new IllegalArgumentException("Este empleado ya existe");
        }
        // Generar password aleatoria
        String generatedPassword = generateRandomPassword(8); // longitud deseada

        UserDTO userDTO = new UserDTO();
        if(employee.getType().equals("RECOLECTOR"))
            userDTO.setRole(Role.EMPLEADO);
        else
            userDTO.setRole(Role.EMPLEADO_TRATADOR);
        userDTO.setEmail(email);
        userDTO.setPassword(generatedPassword); // <-- asignamos la password

        UserDTO userSave = userService.registerUser(userDTO);
        EmployeesEntity entity = modelMapper.map(employee, EmployeesEntity.class);
        entity.setUser(modelMapper.map(userSave, UserEntity.class));
        entity.setState("ACTIVO");
        EmployeesEntity entitySaved = employeeRepository.save(entity);



        envioCorreosService.enviarCorreo(email, "Cuenta creada:" + Role.EMPLEADO.name(),
                "Se a creado el usuario para el sistema de Veolia \n" +
                        "Usuario: " + email + "\n"+
                        "Contraseña: " + generatedPassword);
        return modelMapper.map(entitySaved, Employee.class);
    }
    private String generateRandomPassword(int length) {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String symbols = "!@#$%^&*()-_";

        String allChars = upper + lower + digits + symbols;
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(allChars.length());
            password.append(allChars.charAt(index));
        }

        return password.toString();
    }
    @Override
    public Employee updateEmployee(Employee employee) {
        return null;
    }

    @Override
    public Employee deleteEmployee(Long id) {
        EmployeesEntity entity = employeeRepository.getReferenceById(id);
        entity.setState("INACTIVO");
        return modelMapper.map(employeeRepository.save(entity), Employee.class);
    }

    @Override
    public List<Employee> getAll() {
        List<Employee> employees = new ArrayList<>();
        List<EmployeesEntity> employeesEntities = employeeRepository.findAll();
        for (EmployeesEntity entity : employeesEntities) {
            employees.add(modelMapper.map(entity, Employee.class));
        }
        return employees;
    }

    @Override
    // Obtener TODAS las hojas de ruta
    public List<EmployeeRoadmapResponse> getAllActiveRoadmaps(Long employeeId) {
        List<RoadmapEntity> roadmaps = roadmapRepository.findAllActiveByEmployee(employeeId);

        if (roadmaps.isEmpty()) {
            throw new RuntimeException("No tienes hojas de ruta asignadas para hoy");
        }

        return roadmaps.stream()
                .map(this::buildRoadmapResponse)
                .collect(Collectors.toList());
    }

    // Obtener UNA hoja de ruta específica
    @Override
    public EmployeeRoadmapResponse getSpecificRoadmap(Long employeeId, Long roadmapId) {
        RoadmapEntity roadmap = roadmapRepository.findSpecificRoadmap(employeeId, roadmapId)
                .orElseThrow(() -> new RuntimeException("Hoja de ruta no encontrada o no te pertenece"));

        return buildRoadmapResponse(roadmap);
    }
    // Método auxiliar para construir la respuesta (reutilizable)
    private EmployeeRoadmapResponse buildRoadmapResponse(RoadmapEntity roadmap) {
        EmployeeRoadmapResponse response = new EmployeeRoadmapResponse();
        response.setRoadmapId(roadmap.getRoadmapId());
        response.setCollectDate(roadmap.getCollectDate());

        // 🆕 USAR LA RELACIÓN CON ZONA
        response.setZone(roadmap.getZoneEntity() != null ?
                roadmap.getZoneEntity().getName() : roadmap.getZone());

        List<GeneratorInRoadmap> generators = new ArrayList<>();

        for (RoadmapDetailEntity detail : roadmap.getDetails()) {
            GeneratorEntity gen = detail.getGeneratorEntity();

            // ✅ BUSCAR LA ORDEN ACTIVA DEL GENERADOR
            Optional<OrdersEntity> activeOrderOpt = ordersRepository.findActiveOrderByGeneratorId(gen.getGeneratorId());

            // Obtener precintos (para información de recolección)
            List<SealEntity> seals = sealRepository.findByGeneratorEntityAndStateIn(
                    gen, Arrays.asList(SealStatus.OCUPADO, SealStatus.RECOLECTADO));

            GeneratorInRoadmap genInfo = new GeneratorInRoadmap();
            genInfo.setGeneratorId(gen.getGeneratorId());
            genInfo.setName(gen.getName());
            genInfo.setAddress(gen.getAddress());
            genInfo.setLatitude(gen.getLatitude());
            genInfo.setLongitude(gen.getLongitude());

            // ✅ USAR INFORMACIÓN DE LA ORDEN SI EXISTE
            if (activeOrderOpt.isPresent()) {
                OrdersEntity activeOrder = activeOrderOpt.get();
                genInfo.setRequestedBags(activeOrder.getCountBags()); // ✅ Cantidad solicitada en la orden
                genInfo.setOrderId(activeOrder.getId()); // ✅ ID de la orden

                // Para compatibilidad, mantener totalBags igual a requestedBags
                genInfo.setTotalBags(activeOrder.getCountBags());

                log.debug("📦 Generador {}: Orden encontrada con {} bolsas solicitadas",
                        gen.getName(), activeOrder.getCountBags());
            } else {
                // ✅ FALLBACK: Si no hay orden, usar cantidad de precintos
                genInfo.setRequestedBags(seals.size());
                genInfo.setTotalBags(seals.size());
                genInfo.setOrderId(null);

                log.warn("⚠️ Generador {}: No se encontró orden activa, usando {} precintos como fallback",
                        gen.getName(), seals.size());
            }

            // ✅ CANTIDAD RECOLECTADA: Siempre basada en precintos RECOLECTADO
            int collectedSeals = (int) seals.stream()
                    .filter(s -> s.getState() == SealStatus.RECOLECTADO).count();
            genInfo.setCollectedBags(collectedSeals);

            // ✅ DETERMINAR ESTADO BASADO EN ORDEN vs RECOLECTADO
            if (collectedSeals == 0) {
                genInfo.setStatus("PENDIENTE");
            } else if (collectedSeals >= genInfo.getRequestedBags()) {
                genInfo.setStatus("COMPLETADO");
            } else {
                genInfo.setStatus("EN_PROCESO");
            }

            // ✅ INFORMACIÓN DE PRECINTOS (mantenida para compatibilidad)
            List<SealInfo> sealInfos = seals.stream()
                    .map(seal -> {
                        SealInfo info = new SealInfo();
                        info.setSealId(seal.getSealId());
                        info.setSealNumber(seal.getSealNumber());
                        info.setState(seal.getState().toString());
                        info.setQrContent(seal.getQrContent());
                        return info;
                    }).collect(Collectors.toList());

            genInfo.setSeals(sealInfos);
            generators.add(genInfo);

            // ✅ LOG PARA DEBUG
            log.debug("📋 Generador {}: Solicitadas={}, Recolectadas={}, Estado={}",
                    gen.getName(), genInfo.getRequestedBags(), collectedSeals, genInfo.getStatus());
        }

        response.setGenerators(generators);
        return response;
    }
    @Override
    public ScanQRResponse processScanQR(ScanQRRequest request) {
        // 1. Parsear QR
        String[] qrParts = request.getQrContent().split("\\|");
        if (qrParts.length != 3) {
            throw new RuntimeException("QR inválido");
        }

        Long sealId = Long.parseLong(qrParts[0]);
        String sealNumber = qrParts[1];
        Long generatorId = Long.parseLong(qrParts[2]);
        // 🆕 AGREGAR ESTE LOG
        System.out.println("🔍 Buscando precinto con ID: " + sealId);
        // 2. Validar que el precinto existe
        SealEntity seal = sealRepository.findById(sealId)
                .orElseThrow(() -> new RuntimeException("Precinto no encontrado"));

        // 3. Validar que el QR coincide con el precinto
        if (!seal.getQrContent().equals(request.getQrContent())) {
            throw new RuntimeException("QR no coincide con el precinto");
        }

        // 4. Validar que está en estado OCUPADO
        if (seal.getState() != SealStatus.OCUPADO) {
            throw new RuntimeException("Este precinto ya fue recolectado o no está disponible");
        }

        // 5. Validar que el generador está en la hoja de ruta del empleado
        boolean generatorInRoadmap = roadmapRepository.existsGeneratorInEmployeeRoadmap(
                request.getEmployeeId(), generatorId, request.getRoadmapId());

        if (!generatorInRoadmap) {
            throw new RuntimeException("Este generador no está en tu hoja de ruta");
        }

        // 6. Cambiar estado a RECOLECTADO
        seal.setState(SealStatus.RECOLECTADO);
        sealRepository.save(seal);

        // 7. Contar cuántos lleva recolectados de este generador
        int newCollectedCount = sealRepository.countByGeneratorEntityAndState(
                seal.getGeneratorEntity(), SealStatus.RECOLECTADO);
        try {
            checkAndCompleteRoadmapIfNeeded(request.getRoadmapId());
        } catch (Exception e) {
            log.warn("⚠️ Error verificando completación de ruta {}: {}", request.getRoadmapId(), e.getMessage());
            // No fallar el escaneo por esto
        }

        // 8. Preparar respuesta
        ScanQRResponse response = new ScanQRResponse();
        response.setSuccess(true);
        response.setMessage("Precinto recolectado exitosamente");
        response.setSealNumber(seal.getSealNumber());
        response.setGeneratorName(seal.getGeneratorEntity().getName());
        response.setNewCollectedCount(newCollectedCount);

        return response;
    }
    private void checkAndCompleteRoadmapIfNeeded(Long roadmapId) {
        RoadmapEntity roadmap = roadmapRepository.findById(roadmapId).orElse(null);
        if (roadmap == null || roadmap.getStatus() != RoadmapStatus.EN_PROGRESO) {
            return;
        }

        // Calcular progreso total
        int totalSeals = 0;
        int collectedSeals = 0;

        for (RoadmapDetailEntity detail : roadmap.getDetails()) {
            List<SealEntity> seals = sealRepository.findByGeneratorEntityAndStateIn(
                    detail.getGeneratorEntity(),
                    Arrays.asList(SealStatus.OCUPADO, SealStatus.RECOLECTADO));

            totalSeals += seals.size();
            collectedSeals += (int) seals.stream()
                    .filter(s -> s.getState() == SealStatus.RECOLECTADO)
                    .count();
        }

        // 🆕 CAMBIO CRÍTICO: Solo completar al 100% (no al 90%)
        double completionPercentage = totalSeals > 0 ? (double) collectedSeals / totalSeals : 0;

        if (completionPercentage >= 1.0) {
            roadmap.setStatus(RoadmapStatus.COMPLETADA);
            roadmapRepository.save(roadmap);

            log.info("✅ Hoja de ruta {} COMPLETADA automáticamente - 100% completado ({}/{})",
                    roadmapId, collectedSeals, totalSeals);
        } else {
            log.debug("📊 Hoja de ruta {} progreso: {}% ({}/{})",
                    roadmapId, Math.round(completionPercentage * 100), collectedSeals, totalSeals);
        }
    }
    @Override
    @Transactional
    public void completeRoadmapManually(Long employeeId, Long roadmapId) {
        RoadmapEntity roadmap = roadmapRepository.findSpecificRoadmap(employeeId, roadmapId)
                .orElseThrow(() -> new RuntimeException("Hoja de ruta no encontrada o no te pertenece"));

        if (roadmap.getStatus() != RoadmapStatus.EN_PROGRESO) {
            throw new IllegalStateException("Solo se pueden completar rutas en progreso");
        }

        // Verificar que tenga precintos recolectados
        Long collectedCount = roadmapRepository.countCollectedSealsByRoadmap(roadmapId);
        if (collectedCount == 0) {
            throw new IllegalStateException("No se puede completar una ruta sin precintos recolectados");
        }

        roadmap.setStatus(RoadmapStatus.COMPLETADA);
        roadmapRepository.save(roadmap);

        log.info("🏁 Hoja de ruta {} completada manualmente - {} precintos recolectados",
                roadmapId, collectedCount);
    }
    @Override
    public EmployeesEntity getEmployeeByUserId(Long userId) {
        return employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado para este usuario"));
    }
    @Override
    @Transactional
    public RoadmapCloseResponse closeRoadmap(Long employeeId, RoadmapCloseRequest request) {
        // 1. Validar que la hoja de ruta existe y pertenece al empleado
        RoadmapEntity roadmap = roadmapRepository.findSpecificRoadmap(employeeId, request.getRoadmapId())
                .orElseThrow(() -> new RuntimeException("Hoja de ruta no encontrada o no te pertenece"));

        // 2. Validar que está en estado COMPLETADA
        if (roadmap.getStatus() == RoadmapStatus.CERRADA) {
            throw new IllegalStateException("Esta hoja de ruta ya está cerrada");
        }

        // 3. Validar datos del formulario
        validateCloseRequest(request, roadmap);

        // 4. Contar precintos que se van a entregar a planta
        Long sealsToDeliver = roadmapRepository.countCollectedSealsByRoadmap(request.getRoadmapId());
        if (sealsToDeliver == 0) {
            throw new IllegalStateException("No hay precintos recolectados para entregar");
        }

        // 5. Actualizar todos los precintos RECOLECTADO → EN_PLANTA
        List<SealEntity> collectedSeals = roadmapRepository.findCollectedSealsByRoadmap(request.getRoadmapId());
        collectedSeals.forEach(seal -> {
            seal.setState(SealStatus.EN_PLANTA);
        });
        sealRepository.saveAll(collectedSeals);

        
        // 6. Actualizar la hoja de ruta
        LocalDateTime now = LocalDateTime.now();
        roadmap.setReturnHour(request.getReturnHour());
        roadmap.setFinalKm(request.getFinalKm());
        roadmap.setObservations(request.getObservations());
        roadmap.setCloseDateTime(now);
        roadmap.setStatus(RoadmapStatus.CERRADA);

        roadmapRepository.save(roadmap);

        // 7. Calcular estadísticas para la respuesta
        RoadmapCloseResponse response = buildCloseResponse(roadmap, sealsToDeliver.intValue());

        String statusMessage = sealsToDeliver > 0 ?
                String.format("con %d precintos entregados", sealsToDeliver) :
                "sin precintos recolectados";

        log.info("🏁 Hoja de ruta {} CERRADA {} por empleado {} - Estado anterior: {}",
                request.getRoadmapId(), statusMessage, employeeId, roadmap.getStatus());

        return response;
    }

    private void validateCloseRequest(RoadmapCloseRequest request, RoadmapEntity roadmap) {
        // Validar hora de retorno
        if (request.getReturnHour() == null) {
            throw new IllegalArgumentException("La hora de retorno es requerida");
        }

        if (roadmap.getExitHour() != null && request.getReturnHour().isBefore(roadmap.getExitHour())) {
            throw new IllegalArgumentException("La hora de retorno no puede ser anterior a la hora de salida");
        }

        // Validar kilometraje final
        if (request.getFinalKm() == null || request.getFinalKm() <= 0) {
            throw new IllegalArgumentException("El kilometraje final es requerido y debe ser mayor a 0");
        }

        if (roadmap.getInitialKm() != null && request.getFinalKm() < roadmap.getInitialKm()) {
            throw new IllegalArgumentException("El kilometraje final no puede ser menor al inicial");
        }
    }

    private RoadmapCloseResponse buildCloseResponse(RoadmapEntity roadmap, Integer sealsDelivered) {
        RoadmapCloseResponse response = new RoadmapCloseResponse();

        response.setSuccess(true);
        response.setMessage("Hoja de ruta cerrada exitosamente");
        response.setRoadmapId(roadmap.getRoadmapId());
        response.setSealsDelivered(sealsDelivered);
        response.setCloseDateTime(roadmap.getCloseDateTime());

        // Calcular kilómetros recorridos
        if (roadmap.getInitialKm() != null && roadmap.getFinalKm() != null) {
            response.setKmTraveled(roadmap.getFinalKm() - roadmap.getInitialKm());
        }

        // Calcular duración del trabajo
        if (roadmap.getExitHour() != null && roadmap.getReturnHour() != null) {
            long workMinutes = java.time.Duration.between(roadmap.getExitHour(), roadmap.getReturnHour()).toMinutes();
            response.setWorkDurationMinutes(workMinutes);
        }

        // Contar generadores
        response.setTotalGenerators(roadmap.getDetails().size());

        return response;
    }
    @Override
    public List<EmployeeRoadmapResponse> getCompletedRoadmaps(Long employeeId) {
        // ✅ CAMBIAR por query más flexible
        List<RoadmapEntity> closeableRoadmaps = roadmapRepository.findRoadmapsReadyToClose(employeeId);

        if (closeableRoadmaps.isEmpty()) {
            throw new RuntimeException("No tienes hojas de ruta para cerrar hoy");
        }

        return closeableRoadmaps.stream()
                .map(this::buildRoadmapResponse)
                .collect(Collectors.toList());
    }
    @Override
    public List<EmployeeRoadmapResponse> getActiveRoadmapsToClose(Long employeeId) {
        List<RoadmapEntity> activeRoadmaps = roadmapRepository.findActiveRoadmapsToClose(employeeId);

        if (activeRoadmaps.isEmpty()) {
            throw new RuntimeException("No tienes hojas de ruta activas para cerrar");
        }

        return activeRoadmaps.stream()
                .map(this::buildRoadmapResponse)
                .collect(Collectors.toList());
    }
    @Override
    public List<EmployeeRoadmapResponse> getCloseableRoadmaps(Long employeeId) {
        List<RoadmapEntity> closeableRoadmaps = roadmapRepository.findRoadmapsReadyToClose(employeeId);

        if (closeableRoadmaps.isEmpty()) {
            throw new RuntimeException("No tienes hojas de ruta para cerrar hoy");
        }

        return closeableRoadmaps.stream()
                .map(this::buildRoadmapResponse)
                .collect(Collectors.toList());
    }
}
