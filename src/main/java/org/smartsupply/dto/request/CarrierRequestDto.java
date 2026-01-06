package org.smartsupply.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CarrierRequestDto {

    @NotBlank(message = "Le nom du transporteur est obligatoire")
    private String name;

    @Email(message = "L'email du transporteur doit être valide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    private String phone;


    @DecimalMin(value = "0.0", inclusive = true, message = "Le tarif d'expédition ne peut pas être négatif")
    private BigDecimal shippingRate;

}
