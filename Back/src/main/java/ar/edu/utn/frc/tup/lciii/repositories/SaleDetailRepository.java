package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.SaleDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleDetailRepository extends JpaRepository<SaleDetailEntity, Long> {
    boolean existsBySale_SaleId(Long saleId);

    // O si prefieres con @Query
    @Query("SELECT COUNT(sd) > 0 FROM SaleDetailEntity sd WHERE sd.sale.saleId = :saleId")
    boolean existsBySaleId(@Param("saleId") Long saleId);
}
