package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestOrderDto {
   private Long zoneId;
   private Long userId;
   private LocalDateTime scheduledDate;
   private Integer countBags;
}
