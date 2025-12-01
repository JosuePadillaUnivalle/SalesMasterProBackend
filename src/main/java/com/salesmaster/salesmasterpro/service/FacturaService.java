package com.salesmaster.salesmasterpro.service;

import com.salesmaster.salesmasterpro.dto.FacturaDTO;
import com.salesmaster.salesmasterpro.dto.PedidoItemDTO;
import com.salesmaster.salesmasterpro.entity.Factura;
import com.salesmaster.salesmasterpro.entity.Pedido;
import com.salesmaster.salesmasterpro.exception.ResourceNotFoundException;
import com.salesmaster.salesmasterpro.repository.FacturaRepository;
import com.salesmaster.salesmasterpro.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final PedidoRepository pedidoRepository;

    public FacturaDTO generarFactura(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + idPedido));

        if (pedido.getFactura() != null) {
            throw new IllegalArgumentException("El pedido ya tiene una factura asociada");
        }

        String numeroFactura = generarNumeroFactura();

        Factura factura = Factura.builder()
                .pedido(pedido)
                .nro(numeroFactura)
                .fecha(LocalDateTime.now())
                .total(pedido.getTotal())
                .build();

        Factura facturaGuardada = facturaRepository.save(factura);
        pedido.setFactura(facturaGuardada);

        return convertirADTO(facturaGuardada);
    }

    @Transactional(readOnly = true)
    public List<FacturaDTO> listarFacturas() {
        List<Factura> facturas = facturaRepository.findAll();
        
        // Forzar carga de relaciones lazy para evitar LazyInitializationException
        facturas.forEach(factura -> {
            if (factura.getPedido() != null) {
                factura.getPedido().getPedidoProductos().size(); // Inicializar colección
                if (factura.getPedido().getCliente() != null) {
                    factura.getPedido().getCliente().getNombre(); // Inicializar relación
                }
            }
        });
        
        return facturas.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FacturaDTO obtenerFacturaPorId(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + id));
        
        // Forzar carga de relaciones lazy para evitar LazyInitializationException
        if (factura.getPedido() != null) {
            factura.getPedido().getPedidoProductos().size(); // Inicializar colección
            if (factura.getPedido().getCliente() != null) {
                factura.getPedido().getCliente().getNombre(); // Inicializar relación
            }
        }
        
        return convertirADTO(factura);
    }

    private String generarNumeroFactura() {
        // Formato compacto: FAC-YYMMDD-NNNN (máximo 15 caracteres)
        // Ejemplo: FAC-251123-0001 = 13 caracteres
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String prefijo = "FAC-" + fecha + "-";
        
        // Contar facturas del día actual
        long count = facturaRepository.countByNroStartingWith(prefijo);
        long numero = count + 1;
        
        // Formato: FAC-YYMMDD-NNNN (máximo 13 caracteres)
        return String.format("FAC-%s-%04d", fecha, numero);
    }

    private FacturaDTO convertirADTO(Factura factura) {
        // Obtener items del pedido asociado
        List<PedidoItemDTO> items = factura.getPedido().getPedidoProductos().stream()
                .map(pp -> PedidoItemDTO.builder()
                        .idProd(pp.getProducto().getIdProd())
                        .cantidad(pp.getCantidad())
                        .subtotal(pp.getSubtotal())
                        .nombreProducto(pp.getProducto().getNombre())
                        .build())
                .collect(Collectors.toList());
        
        return FacturaDTO.builder()
                .idFactura(factura.getIdFactura())
                .idPedido(factura.getPedido().getIdPedido())
                .nro(factura.getNro())
                .fecha(factura.getFecha())
                .total(factura.getTotal())
                .nombreCliente(factura.getPedido().getCliente().getNombre())
                .items(items)
                .build();
    }
}

