package org.smartsupply.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseRequestDto {
    @NotBlank(message = "Le code de l'entrepôt est obligatoire")
    private String code;

    @NotBlank(message = "Le nom de l'entrepôt est obligatoire")
    private String name;

    private Boolean active = true;



    private String managerEmail;
}