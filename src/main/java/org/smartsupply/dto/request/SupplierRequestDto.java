package org.smartsupply.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRequestDto {
    @NotBlank(message = "Le nom du fournisseur est obligatoire")
    @Size(min = 2, max = 200)
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Le contact est obligatoire")
    @Size(min = 3, max = 100)
    private String contact;
}