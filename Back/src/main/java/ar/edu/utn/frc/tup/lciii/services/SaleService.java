package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.dtos.SaleDto;
import ar.edu.utn.frc.tup.lciii.models.Sale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public interface SaleService {
    Sale registerSale(SaleDto saleDto);

    @Transactional
    void confirmSale(Long saleId, Long generatorId);

    List<Sale> findByGenerator(Long id);
    Long getLastPendingSaleId(Long generatorId);
}
