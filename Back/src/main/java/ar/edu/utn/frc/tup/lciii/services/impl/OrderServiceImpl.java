package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.dtos.OrdersDto;
import ar.edu.utn.frc.tup.lciii.dtos.RequestOrderDto;
import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.OrdersEntity;
import ar.edu.utn.frc.tup.lciii.entities.UserEntity;
import ar.edu.utn.frc.tup.lciii.entities.ZoneEntity;
import ar.edu.utn.frc.tup.lciii.models.SealStatus;
import ar.edu.utn.frc.tup.lciii.repositories.GeneratorRepository;
import ar.edu.utn.frc.tup.lciii.repositories.OrdersRepository;
import ar.edu.utn.frc.tup.lciii.repositories.SealRepository;
import ar.edu.utn.frc.tup.lciii.repositories.UserRepository;
import ar.edu.utn.frc.tup.lciii.repositories.ZoneRepository;
import ar.edu.utn.frc.tup.lciii.services.OrderService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private OrdersRepository ordersRepository;
    @Autowired
    private GeneratorRepository generatorRepository;
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SealRepository sealRepository;
    @Override
    public OrdersDto createOrder(RequestOrderDto requestOrderDto) {
        try {
            System.out.println("🔍 Creando orden con datos: " + requestOrderDto);

            if (requestOrderDto == null) {
                throw new IllegalArgumentException("RequestOrderDto no puede ser null");
            }
            GeneratorEntity generatorEntity = generatorRepository.findByUserId(requestOrderDto.getUserId());

            // Validar generatorId
            if (generatorEntity.getGeneratorId() == null) {
                throw new IllegalArgumentException("GeneratorId no puede ser null");
            }

            // Validar que el generador exista
            Optional<GeneratorEntity> generatorOpt = generatorRepository.findById(generatorEntity.getGeneratorId());
            if (generatorOpt.isEmpty()) {
                throw new IllegalArgumentException("Generador no encontrado con ID: " + generatorEntity.getGeneratorId());
            }
            GeneratorEntity generator = generatorOpt.get();
            System.out.println("✅ Generador encontrado: " + generator.getName());

            // Validar zoneId
            if (requestOrderDto.getZoneId() == null) {
                throw new IllegalArgumentException("ZoneId no puede ser null");
            }

            Optional<ZoneEntity> zoneOpt = zoneRepository.findById(requestOrderDto.getZoneId());
            if (zoneOpt.isEmpty()) {
                throw new IllegalArgumentException("Zona no encontrada con ID: " + requestOrderDto.getZoneId());
            }
            ZoneEntity zone = zoneOpt.get();
            System.out.println("✅ Zona encontrada: " + zone.getName());

            if (requestOrderDto.getScheduledDate() == null) {
                throw new IllegalArgumentException("ScheduledDate no puede ser null");
            }
            System.out.println("✅ Fecha programada: " + requestOrderDto.getScheduledDate());
            if (requestOrderDto.getCountBags() == null || requestOrderDto.getCountBags() <= 0) {
                throw new IllegalArgumentException("La cantidad de bolsas debe ser mayor a 0");
            }

            // ✅ VALIDAR QUE EL GENERADOR TENGA SUFICIENTES PRECINTOS OCUPADOS
            int precintosOcupados = sealRepository.countByGeneratorEntityAndState(generator, SealStatus.OCUPADO);
            System.out.println("🔍 Precintos OCUPADOS del generador: " + precintosOcupados);
            System.out.println("🔍 Cantidad de bolsas solicitadas: " + requestOrderDto.getCountBags());

            if (precintosOcupados < requestOrderDto.getCountBags()) {
                throw new IllegalArgumentException(
                        String.format("El generador no tiene suficientes precintos. Precintos disponibles: %d, Bolsas solicitadas: %d. " +
                                        "Debe comprar más bolsas antes de solicitar el retiro.",
                                precintosOcupados, requestOrderDto.getCountBags())
                );
            }
            // Crear y configurar la entidad
            OrdersEntity ordersEntity = new OrdersEntity();
            ordersEntity.setGeneratorEntity(generator);
            ordersEntity.setState("PENDIENTE");
            ordersEntity.setZoneEntity(zone);
            ordersEntity.setScheduledDate(requestOrderDto.getScheduledDate());
            ordersEntity.setCountBags(requestOrderDto.getCountBags()); // ¡Faltaba esto!

            // Guardar la entidad
            System.out.println("💾 Guardando orden...");
            OrdersEntity savedOrder = ordersRepository.save(ordersEntity);
            System.out.println("✅ Orden guardada con ID: " + savedOrder.getId());

            // MAPEO MANUAL - Evitar problemas del ModelMapper
            OrdersDto result = mapOrderEntityToDto(savedOrder);
            System.out.println("✅ Orden creada exitosamente: " + result);

            return result;

        } catch (IllegalArgumentException e) {
            // Re-lanzar errores de validación con el mensaje original
            System.err.println("❌ Error de validación: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            // Log del error real para debugging
            System.err.println("❌ Error inesperado al crear orden: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException("Error al crear orden: " + e.getMessage(), e);
        }
    }

    /**
     * Mapeo manual de OrdersEntity a OrdersDto
     * Evita problemas con ModelMapper cuando hay entidades relacionadas
     */
    private OrdersDto mapOrderEntityToDto(OrdersEntity entity) {
        OrdersDto dto = new OrdersDto();

        // Campos básicos
        dto.setId(entity.getId());
        dto.setState(entity.getState());
        dto.setScheduledDate(entity.getScheduledDate());
        dto.setCreationDate(LocalDateTime.now()); // Fecha de creación actual

        // Información del generador
        if (entity.getGeneratorEntity() != null) {
            dto.setGenerador(entity.getGeneratorEntity().getName());
            dto.setAddress(entity.getGeneratorEntity().getAddress());
            dto.setContact(entity.getGeneratorEntity().getContact());
            dto.setEmail(entity.getGeneratorEntity().getEmail());
            dto.setGeneratorType(entity.getGeneratorEntity().getType());

            // Coordenadas si están disponibles
            dto.setLatitude(entity.getGeneratorEntity().getLatitude());
            dto.setLongitude(entity.getGeneratorEntity().getLongitude());
        }

        // Información de la zona
        if (entity.getZoneEntity() != null) {
            dto.setZone(entity.getZoneEntity().getName());
        }

        // Información del empleado (si está asignado)
        if (entity.getEmployeeEntity() != null) {
            dto.setEmployeeId(entity.getEmployeeEntity().getId());
            dto.setEmployeeName(entity.getEmployeeEntity().getName());
            dto.setEmployeeState(entity.getEmployeeEntity().getState());
        }

        // Campos calculados
        dto.setHasValidCoordinates(dto.getLatitude() != null && dto.getLongitude() != null);
        dto.setPriority(1); // Prioridad por defecto
        dto.setPriorityText("Normal");

        return dto;
    }
}
