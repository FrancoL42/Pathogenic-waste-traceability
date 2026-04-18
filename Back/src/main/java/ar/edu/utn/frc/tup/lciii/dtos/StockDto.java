package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockDto {
    private Long bagId;
    private String size;
    private Double price;
    private Integer currentStock;
    private boolean isLowStock;
    private Integer minStock = 500;
}
