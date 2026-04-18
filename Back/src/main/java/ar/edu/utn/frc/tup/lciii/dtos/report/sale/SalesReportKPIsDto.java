package ar.edu.utn.frc.tup.lciii.dtos.report.sale;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportKPIsDto {
    private Integer totalVentasGlobal;
    private String generadorConMasVentas;
    private Integer promedioBolsasPorPeriodo;
    private Double porcentajeTipoPublico;
    private Double porcentajeTipoPrivado;
    private Double tendenciaCrecimiento;
    private Double montoTotalVentas;
}