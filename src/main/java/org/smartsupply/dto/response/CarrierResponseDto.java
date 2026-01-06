package org.smartsupply.dto.response;


import lombok.*;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarrierResponseDto {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private BigDecimal shippingRate;

}
