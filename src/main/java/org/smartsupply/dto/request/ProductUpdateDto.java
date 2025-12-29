package org.smartsupply.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateDto {

    @NotBlank(message = "Le SKU est obligatoire")
    @Size(min = 2, max = 50, message = "Le SKU doit contenir entre 2 et 50 caractères")
    private String sku;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(min = 2, max = 200, message = "Le nom doit contenir entre 2 et 200 caractères")
    private String name;

    @NotNull(message = "Le nom du catégorie est obligatoire")
    private String categoryName;

    @NotNull(message = "Le prix d'origine est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix d'origine doit être supérieur à 0")
    private BigDecimal originalPrice;

    @NotNull(message = "Le profit est obligatoire")
    @DecimalMin(value = "0.0", message = "Le profit ne peut pas être négatif")
    private BigDecimal profit;

    @NotBlank(message = "L'unité est obligatoire")
    @Size(max = 20, message = "L'unité ne peut pas dépasser 20 caractères")
    private String unit;

    private Boolean active;
}