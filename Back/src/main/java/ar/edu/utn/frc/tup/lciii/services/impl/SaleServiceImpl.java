package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.dtos.SaleDto;
import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.SaleDetailEntity;
import ar.edu.utn.frc.tup.lciii.entities.SalesEntity;
import ar.edu.utn.frc.tup.lciii.entities.SealEntity;
import ar.edu.utn.frc.tup.lciii.models.Sale;
import ar.edu.utn.frc.tup.lciii.models.SealStatus;
import ar.edu.utn.frc.tup.lciii.repositories.GeneratorRepository;
import ar.edu.utn.frc.tup.lciii.repositories.SaleDetailRepository;
import ar.edu.utn.frc.tup.lciii.repositories.SaleRepository;
import ar.edu.utn.frc.tup.lciii.repositories.SealRepository;
import ar.edu.utn.frc.tup.lciii.services.SaleService;
import ar.edu.utn.frc.tup.lciii.services.StockService;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SaleServiceImpl implements SaleService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private SaleRepository saleRepository;
    @Autowired
    private SaleDetailRepository saleDetailRepository;
    @Autowired
    private GeneratorRepository generatorRepository;
    @Autowired
    private SealRepository sealRepository;
    @Autowired
    private StockService stockService;

    /**
     * Registra una venta en estado PENDING, sin asignarle aún un generador ni precintos.
     */
    @Override
    @Transactional
    public Sale registerSale(SaleDto saleDto) {
        // 1) Mapeo DTO ➔ entidad
        SalesEntity sale = new SalesEntity();
        sale.setTotalBuy(saleDto.getTotalBuy());
        sale.setQuantity(saleDto.getQuantity());
        sale.setStatus("PENDING");
        sale.setDate(LocalDateTime.now());
        sale.setGeneratorEntity(generatorRepository.getReferenceById(saleDto.getGeneratorId()));
        sale.setCapitulationDate(LocalDateTime.now());
        // 2) Guardo la venta
        SalesEntity saved = saleRepository.save(sale);
        // 3) Retorno DTO mapeado
        return modelMapper.map(saved, Sale.class);
    }

    @Override
    public Long getLastPendingSaleId(Long generatorId) {
        try {
            List<SalesEntity> pendingSales = saleRepository.findPendingSalesByGenerator(
                    generatorId, "PENDING"
            );

            if (!pendingSales.isEmpty()) {
                System.out.println("Encontrada venta pendiente - Sale ID: " + pendingSales.get(0).getSaleId());
                return pendingSales.get(0).getSaleId();
            }

            System.out.println("No se encontraron ventas pendientes para Generator ID: " + generatorId);
            return null;
        } catch (Exception e) {
            System.err.println("Error buscando última venta pendiente: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Confirma una venta: asigna un generador y le reserva N precintos.
     */
    @Override
    @Transactional
    public void confirmSale(Long saleId, Long generatorId) {
        try {
            System.out.println("=== CONFIRMANDO VENTA ===");
            System.out.println("Sale ID: " + saleId + ", Generator ID: " + generatorId);

            // Validaciones básicas
            if (saleId == null || generatorId == null) {
                throw new IllegalArgumentException("Sale ID y Generator ID no pueden ser null");
            }

            SalesEntity sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada"));
            GeneratorEntity gen = generatorRepository.findById(generatorId)
                    .orElseThrow(() -> new EntityNotFoundException("Generador no encontrado"));

            // Si ya está PAID, no hacer nada
            if ("PAID".equals(sale.getStatus())) {
                System.out.println("⚠️ Venta ya está PAID");
                return;
            }

            System.out.println("Cantidad de bolsas: " + sale.getQuantity());

            // 1) Buscar precintos DISPONIBLES del generador
            List<SealEntity> precintosDisponibles = sealRepository.findByStateOrderBySealNumberAsc(
                     SealStatus.DISPONIBLE, PageRequest.of(0, sale.getQuantity())
            );

            System.out.println("Precintos disponibles encontrados: " + precintosDisponibles.size());

            if (precintosDisponibles.size() < sale.getQuantity()) {
                throw new IllegalArgumentException("No hay suficientes precintos disponibles");
            }

            Long bagId = determineBagId(sale); // Método auxiliar para determinar qué tipo de bolsa
            if (bagId != null) {
                stockService.decrementStock(bagId, sale.getQuantity());
                System.out.println("✅ Stock decrementado - BagID: " + bagId + ", Cantidad: " + sale.getQuantity());
            }

            // 2) Cambiar estado de la VENTA a PAID
            sale.setStatus("PAID");
            sale.setCapitulationDate(LocalDateTime.now());
            saleRepository.save(sale);
            System.out.println("✅ Venta cambiada a PAID");

            // 3) Cambiar estado de PRECINTOS a OCUPADO
            precintosDisponibles.forEach(precinto -> {
                precinto.setGeneratorEntity(gen);
                precinto.setState(SealStatus.OCUPADO);

                // 🆕 GENERAR QR CONTENT
                String qrContent = String.format("%d|%s|%d",
                        precinto.getSealId(),
                        precinto.getSealNumber(),
                        gen.getGeneratorId());
                precinto.setQrContent(qrContent);

                System.out.println("Precinto " + precinto.getSealNumber() + " → OCUPADO con QR: " + qrContent);
            });
            sealRepository.saveAll(precintosDisponibles);
            System.out.println("✅ " + precintosDisponibles.size() + " precintos cambiados a OCUPADO");

            System.out.println("=== VENTA CONFIRMADA EXITOSAMENTE ===");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    private Long determineBagId(SalesEntity sale) {
        double precioUnitario = sale.getTotalBuy() / sale.getQuantity();

        if (precioUnitario >= 1500) {
            return 1L;
        } else {
            return 2L;
        }
    }

    @Override
    public List<Sale> findByGenerator(Long id) {
        GeneratorEntity generatorEntity = generatorRepository.getReferenceById(id);
        List<SalesEntity> salesEntities = saleRepository.findAllByGeneratorEntity(generatorEntity);
        List<Sale> sales = new ArrayList<>();
        for (SalesEntity saleEntity : salesEntities) {
            Sale sale = modelMapper.map(saleEntity, Sale.class);
            sale.setDate(saleEntity.getDate().toLocalDate());
            sales.add(sale);
        }
        return sales;
    }
}
