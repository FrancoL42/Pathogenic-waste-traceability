package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.dtos.StockDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface StockService {
    List<StockDto> getAllStock();
    void decrementStock(Long bagId, Integer quantity);
    void incrementStock(Long bagId, Integer quantity);
    List<StockDto> getLowStock();
    void requestRestock(Long bagId);
}
