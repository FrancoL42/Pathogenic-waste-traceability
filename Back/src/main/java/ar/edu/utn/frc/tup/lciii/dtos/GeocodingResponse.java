package ar.edu.utn.frc.tup.lciii.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeocodingResponse {
    private String lat;
    private String lon;
    private String displayName;
    private String country;
    private String city;
    private String state;
}
