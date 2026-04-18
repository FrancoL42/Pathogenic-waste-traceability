package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.ContainerDTO;
import ar.edu.utn.frc.tup.lciii.dtos.PesoPrecintoRequest;
import ar.edu.utn.frc.tup.lciii.dtos.SealTreatmentInfo;
import ar.edu.utn.frc.tup.lciii.entities.ContainersEntity;
import ar.edu.utn.frc.tup.lciii.entities.SealEntity;
import ar.edu.utn.frc.tup.lciii.models.ContainerStatus;
import ar.edu.utn.frc.tup.lciii.models.SealStatus;
import ar.edu.utn.frc.tup.lciii.repositories.SealRepository;
import ar.edu.utn.frc.tup.lciii.services.ContainerService;
import ar.edu.utn.frc.tup.lciii.services.impl.DTOMapperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/treatment")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class ContainerController {

    @Autowired
    private SealRepository sealRepository;

    @Autowired
    private ContainerService containerService;

    @Autowired
    private DTOMapperService dtoMapper;

    // Obtener precintos disponibles para tratamiento
    @GetMapping("/seals/available")
    public ResponseEntity<List<SealTreatmentInfo>> obtenerPrecintosDisponibles() {
        try {
            log.info("🔍 Obteniendo precintos disponibles para tratamiento...");

            // Solo precintos en estado RECOLECTADO (listos para tratamiento)
            List<SealEntity> precintos = sealRepository.findByState(SealStatus.EN_PLANTA);

            log.info("📦 Precintos RECOLECTADOS encontrados: {}", precintos.size());

//            // Filtrar además por peso (solo precintos pesados)
//            List<SealEntity> precintosConPeso = precintos.stream()
//                    .filter(seal -> seal.getPeso() != null && seal.getPeso() > 0)
//                    .collect(Collectors.toList());


            List<SealTreatmentInfo> precintosDTO = dtoMapper.convertSealsToTreatmentInfo(precintos);

            log.info("✅ Devolviendo {} precintos disponibles", precintosDTO.size());
            return ResponseEntity.ok(precintosDTO);

        } catch (Exception e) {
            log.error("❌ Error obteniendo precintos disponibles: {}", e.getMessage(), e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    // Obtener contenedores
    @GetMapping("/containers")
    public ResponseEntity<List<ContainerDTO>> obtenerContenedores(
            @RequestParam(required = false) ContainerStatus estado) {
        List<ContainersEntity> containers = estado != null
                ? containerService.listarPorEstado(estado)
                : containerService.listarTodos();

        List<ContainerDTO> containersDTO = dtoMapper.convertContainersToDTO(containers);
        return ResponseEntity.ok(containersDTO);
    }

    // Crear nuevo contenedor
    @PostMapping("/containers")
    public ResponseEntity<ContainerDTO> crearContenedor(@RequestParam Double pesoMaximo) {
        ContainersEntity container = containerService.crearContenedor(pesoMaximo);
        ContainerDTO containerDTO = dtoMapper.convertContainerToDTO(container);
        return ResponseEntity.ok(containerDTO);
    }

    // Agregar precintos a contenedor
    @PostMapping("/containers/{containerId}/add-seals")
    public ResponseEntity<Map<String, Object>> agregarPrecintos(
            @PathVariable Long containerId,
            @RequestBody List<Long> sealIds) {
        try {
            log.info("🔍 Agregando precintos al contenedor {}: {}", containerId, sealIds);

            containerService.agregarPrecintosAContenedor(containerId, sealIds);

            // Crear respuesta estructurada
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Precintos agregados exitosamente");
            response.put("containerId", containerId);
            response.put("addedSealsCount", sealIds.size());
            response.put("addedSealIds", sealIds);

            log.info("✅ {} precintos agregados exitosamente al contenedor {}", sealIds.size(), containerId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error agregando precintos al contenedor {}: {}", containerId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("containerId", containerId);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Cerrar contenedor
    @PutMapping("/containers/{containerId}/close")
    public ResponseEntity<Map<String, Object>> cerrarContenedor(@PathVariable Long containerId) {
        try {
            log.info("🔍 Cerrando contenedor: {}", containerId);

            containerService.cerrarContenedor(containerId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Contenedor cerrado exitosamente");
            response.put("containerId", containerId);

            log.info("✅ Contenedor {} cerrado exitosamente", containerId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error cerrando contenedor {}: {}", containerId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("containerId", containerId);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Abrir contenedor tratado

    // Método actualizado para procesar tratamiento y reutilizar contenedor
    @PutMapping("/containers/{containerId}/open-treated")
    public ResponseEntity<Map<String, Object>> procesarTratamientoYReutilizar(@PathVariable Long containerId) {
        try {
            log.info("🔍 Procesando tratamiento y reutilizando contenedor: {}", containerId);

            containerService.abrirContenedorTratado(containerId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tratamiento completado. Contenedor reutilizable");
            response.put("containerId", containerId);
            response.put("newState", "ABIERTO");
            response.put("newWeight", 0.0);

            log.info("✅ Contenedor {} procesado y listo para reutilización", containerId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error procesando tratamiento del contenedor {}: {}", containerId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("containerId", containerId);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/seals/{sealId}/weight")
    public ResponseEntity<Map<String, Object>> pesarPrecinto(
            @PathVariable Long sealId,
            @RequestParam Double peso) {
        try {
            log.info("🔍 Pesando precinto: {} con peso: {} kg", sealId, peso);

            // Buscar el precinto
            SealEntity seal = sealRepository.findById(sealId)
                    .orElseThrow(() -> new RuntimeException("Precinto no encontrado"));

            // Validar que esté en estado RECOLECTADO
            if (seal.getState() != SealStatus.EN_PLANTA) {
                throw new RuntimeException("Solo se pueden pesar precintos recolectados");
            }

            // Validar peso
            if (peso <= 0 || peso > 50) {
                throw new RuntimeException("Peso inválido. Debe estar entre 0.1 y 50 kg");
            }

            // Actualizar peso
            seal.setPeso(peso);
            sealRepository.save(seal);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Precinto pesado exitosamente");
            response.put("sealId", sealId);
            response.put("peso", peso);
            response.put("qrContent", seal.getQrContent());

            log.info("✅ Precinto {} pesado exitosamente: {} kg", sealId, peso);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error pesando precinto {}: {}", sealId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("sealId", sealId);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Endpoint para pesar múltiples precintos
    @PutMapping("/seals/weight-batch")
    public ResponseEntity<String> pesarPrecintosLote(@RequestBody List<PesoPrecintoRequest> pesajes) {
        try {
            int pesados = 0;
            for (PesoPrecintoRequest pesaje : pesajes) {
                SealEntity seal = sealRepository.findById(pesaje.getSealId())
                        .orElseThrow(() -> new RuntimeException("Precinto no encontrado: " + pesaje.getSealId()));

                if (seal.getState() == SealStatus.RECOLECTADO && pesaje.getPeso() > 0) {
                    seal.setPeso(pesaje.getPeso());
                    sealRepository.save(seal);
                    pesados++;
                }
            }

            return ResponseEntity.ok("Se pesaron " + pesados + " precintos exitosamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    @PostMapping("/containers/{containerId}/remove-seals")
    public ResponseEntity<Map<String, Object>> removerPrecintos(
            @PathVariable Long containerId,
            @RequestBody List<Long> sealIds) {
        try {
            log.info("🔍 Request para remover precintos: contenedor={}, precintos={}", containerId, sealIds);

            containerService.removerPrecintosDeContenedor(containerId, sealIds);

            // Crear respuesta estructurada
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Precintos removidos exitosamente");
            response.put("containerId", containerId);
            response.put("removedSealsCount", sealIds.size());
            response.put("removedSealIds", sealIds);

            log.info("✅ {} precintos removidos exitosamente del contenedor {}", sealIds.size(), containerId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error removiendo precintos del contenedor {}: {}", containerId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("containerId", containerId);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}