package com.salesmaster.salesmasterpro.dto;

import com.salesmaster.salesmasterpro.validation.ValidEmail;
import com.salesmaster.salesmasterpro.validation.ValidNombre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteDTO {

    private Long idCliente;

    @NotBlank(message = "El nombre es obligatorio")
    @ValidNombre(message = "El nombre solo puede contener letras, espacios, acentos y guiones. Mínimo 2 caracteres.")
    @Size(max = 80, message = "El nombre no puede exceder 80 caracteres")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @ValidEmail(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;
}

