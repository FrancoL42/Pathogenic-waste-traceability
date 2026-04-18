package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScanQRResponse {
    private boolean success;
    private String message;
    private String sealNumber;
    private String generatorName;
    private Integer newCollectedCount;
}
