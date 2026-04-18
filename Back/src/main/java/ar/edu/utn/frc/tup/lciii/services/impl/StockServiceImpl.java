package ar.edu.utn.frc.tup.lciii.services.impl;

import ar.edu.utn.frc.tup.lciii.dtos.StockDto;
import ar.edu.utn.frc.tup.lciii.entities.BagEntity;
import ar.edu.utn.frc.tup.lciii.repositories.BagRepository;
import ar.edu.utn.frc.tup.lciii.services.StockService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockServiceImpl implements StockService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private BagRepository bagRepository;
    @Autowired
    private EnvioCorreosServiceImpl emailService;
    @Override
    public List<StockDto> getAllStock() {
        List<BagEntity> bags = bagRepository.findAll();
        List<StockDto> stockList = new ArrayList<>();

        for (BagEntity bag : bags) {
            StockDto stockDto = new StockDto();
            stockDto.setBagId(bag.getId());
            stockDto.setSize(bag.getSize());
            stockDto.setPrice(bag.getPrice());
            stockDto.setCurrentStock(bag.getStock());
            stockDto.setLowStock(bag.getStock() < 500);
            stockDto.setMinStock(500);
            stockList.add(stockDto);
        }

        return stockList;
    }

    @Override
    @Transactional
    public void decrementStock(Long bagId, Integer quantity) {
        BagEntity bag = bagRepository.findById(bagId)
                .orElseThrow(() -> new IllegalArgumentException("Bolsa no encontrada"));

        if (bag.getStock() < quantity) {
            throw new IllegalArgumentException("Stock insuficiente. Stock actual: " + bag.getStock());
        }

        bag.setStock(bag.getStock() - quantity);
        bagRepository.save(bag);

        System.out.println("Stock decrementado: Bolsa " + bag.getSize() +
                ", Cantidad: " + quantity +
                ", Stock restante: " + bag.getStock());
    }

    @Override
    @Transactional
    public void incrementStock(Long bagId, Integer quantity) {
        BagEntity bag = bagRepository.findById(bagId)
                .orElseThrow(() -> new IllegalArgumentException("Bolsa no encontrada"));

        bag.setStock(bag.getStock() + quantity);
        bagRepository.save(bag);

        System.out.println("Stock incrementado: Bolsa " + bag.getSize() +
                ", Cantidad: " + quantity +
                ", Stock actual: " + bag.getStock());
    }

    @Override
    public List<StockDto> getLowStock() {
        List<StockDto> allStock = getAllStock();
        return allStock.stream()
                .filter(StockDto::isLowStock)
                .toList();
    }

    @Override
    public void requestRestock(Long bagId) {
        BagEntity bag = bagRepository.findById(bagId)
                .orElseThrow(() -> new IllegalArgumentException("Bolsa no encontrada"));

        String subject = "URGENTE: Solicitud de Reposición de Bolsas - " + bag.getSize();
        String body = construirEmailReposicion(bag);

        emailService.enviarCorreo("killbenlentini1@gmail.com", subject, body);

        System.out.println("Email de reposición enviado para bolsas " + bag.getSize() +
                " - Stock actual: " + bag.getStock());
    }

    private String construirEmailReposicion(BagEntity bag) {
        StringBuilder body = new StringBuilder();
        body.append("=== SOLICITUD DE REPOSICIÓN DE BOLSAS ===\n\n");
        body.append("Estimado Equipo de Compras,\n\n");
        body.append("Se requiere reposición urgente de bolsas debido a stock bajo.\n\n");
        body.append("DETALLES:\n");
        body.append("- Tipo de Bolsa: ").append(bag.getSize()).append("\n");
        body.append("- Stock Actual: ").append(bag.getStock()).append(" unidades\n");
        body.append("- Stock Mínimo: ").append(500).append(" unidades\n");
        body.append("Por favor confirmar la orden de compra lo antes posible.\n\n");
        body.append("Saludos,\n");
        body.append("Sistema de Gestión de Residuos - VEOLIA\n");
        body.append("Generado automáticamente el: ").append(LocalDateTime.now());

        return body.toString();
    }
}
