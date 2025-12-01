package com.salesmaster.salesmasterpro.dto;

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
public class FacturaDTO {

    private Long idFactura;
    private Long idPedido;
    private String nro;
    private LocalDateTime fecha;
    private BigDecimal total;
    private String nombreCliente;
    private List<PedidoItemDTO> items;
}

