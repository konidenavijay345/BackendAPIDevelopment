package com.example.backendapi.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Immutable input for product creation and replacement.
 *
 * <p>A separate request DTO prevents clients from setting protected fields such as owner, ID,
 * and timestamps. Validation gives users clear feedback before database work begins.</p>
 */
public record ProductRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 64) String sku,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull @Min(0) Integer quantity
) {
}
