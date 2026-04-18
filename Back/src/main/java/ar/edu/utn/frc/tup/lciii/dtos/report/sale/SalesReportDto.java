package ar.edu.utn.frc.tup.lciii.dtos.report.sale;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportDto {
    private String generador;
    private String tipoGenerador;
    private String zona;
    private Integer totalVentas;
    private Integer cantidadBolsas;
    private Double montoTotal;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaUltimaVenta;
    private Double promedioVentasPorMes;
    private Double crecimientoMensual;
}