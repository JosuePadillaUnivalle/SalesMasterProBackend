package com.salesmaster.salesmasterpro.dto;

import com.salesmaster.salesmasterpro.validation.ValidNombre;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDTO {

    private Long idProd;

    @NotBlank(message = "El nombre es obligatorio")
    @ValidNombre(message = "El nombre solo puede contener letras, espacios, acentos y guiones. Mínimo 2 caracteres.")
    @Size(max = 80, message = "El nombre no puede exceder 80 caracteres")
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @DecimalMax(value = "100000.00", message = "El precio no puede superar los $100,000")
    private BigDecimal precio;
}

