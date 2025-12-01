package com.salesmaster.salesmasterpro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para crear y consultar pedidos")
public class PedidoDTO {

    @Schema(description = "ID del pedido", example = "1")
    private Long idPedido;

    @NotNull(message = "El ID del cliente es obligatorio")
    @Schema(description = "ID del cliente", required = true, example = "1")
    private Long idCliente;

    @Schema(description = "Nombre del cliente", example = "Juan Pérez")
    private String nombreCliente;
    
    @Schema(description = "Fecha del pedido")
    private LocalDateTime fecha;
    
    @Schema(description = "Total del pedido", example = "1299.99")
    private BigDecimal total;

    @NotEmpty(message = "El pedido debe tener al menos un producto")
    @Valid
    @Schema(description = "Lista de productos del pedido", required = true)
    private List<PedidoItemDTO> items;
}

