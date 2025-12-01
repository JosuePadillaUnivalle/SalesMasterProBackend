package com.salesmaster.salesmasterpro.service;

import com.salesmaster.salesmasterpro.dto.PedidoDTO;
import com.salesmaster.salesmasterpro.dto.PedidoItemDTO;
import com.salesmaster.salesmasterpro.entity.*;
import com.salesmaster.salesmasterpro.exception.ResourceNotFoundException;
import com.salesmaster.salesmasterpro.repository.ClienteRepository;
import com.salesmaster.salesmasterpro.repository.PedidoProductoRepository;
import com.salesmaster.salesmasterpro.repository.PedidoRepository;
import com.salesmaster.salesmasterpro.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PedidoProductoRepository pedidoProductoRepository;

    public PedidoDTO crearPedido(PedidoDTO pedidoDTO) {
        // Validación de límite de items por pedido (máximo 100 unidades)
        int totalItems = pedidoDTO.getItems().stream()
                .mapToInt(item -> item.getCantidad())
                .sum();

        if (totalItems > 100) {
            throw new IllegalArgumentException("El pedido no puede superar los 100 artículos en total. Cantidad actual: " + totalItems);
        }

        Cliente cliente = clienteRepository.findById(pedidoDTO.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + pedidoDTO.getIdCliente()));

        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .fecha(LocalDateTime.now())
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal totalPedido = BigDecimal.ZERO;

        for (PedidoItemDTO itemDTO : pedidoDTO.getItems()) {
            Producto producto = productoRepository.findById(itemDTO.getIdProd())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + itemDTO.getIdProd()));

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(itemDTO.getCantidad()));
            totalPedido = totalPedido.add(subtotal);

            // Crear PedidoProductoId con idProd, idPedido se establecerá automáticamente por @MapsId
            PedidoProductoId pedidoProductoId = new PedidoProductoId();
            pedidoProductoId.setIdProd(producto.getIdProd());
            // idPedido se establecerá automáticamente cuando se guarde el pedido
            
            PedidoProducto pedidoProducto = PedidoProducto.builder()
                    .id(pedidoProductoId)
                    .pedido(pedido)
                    .producto(producto)
                    .cantidad(itemDTO.getCantidad())
                    .subtotal(subtotal)
                    .build();

            pedido.getPedidoProductos().add(pedidoProducto);
        }

        pedido.setTotal(totalPedido);
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        return convertirADTO(pedidoGuardado);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPedidos() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        
        // Forzar carga de relaciones lazy para evitar LazyInitializationException
        pedidos.forEach(pedido -> {
            if (pedido.getCliente() != null) {
                pedido.getCliente().getNombre(); // Inicializar relación
            }
            if (pedido.getPedidoProductos() != null) {
                pedido.getPedidoProductos().forEach(pp -> {
                    if (pp.getProducto() != null) {
                        pp.getProducto().getNombre(); // Inicializar relación
                    }
                });
            }
        });
        
        return pedidos.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PedidoDTO obtenerPedidoPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));
        return convertirADTO(pedido);
    }

    private PedidoDTO convertirADTO(Pedido pedido) {
        List<PedidoItemDTO> items = pedido.getPedidoProductos().stream()
                .map(pp -> PedidoItemDTO.builder()
                        .idProd(pp.getProducto().getIdProd())
                        .cantidad(pp.getCantidad())
                        .subtotal(pp.getSubtotal())
                        .nombreProducto(pp.getProducto().getNombre())
                        .build())
                .collect(Collectors.toList());

        return PedidoDTO.builder()
                .idPedido(pedido.getIdPedido())
                .idCliente(pedido.getCliente().getIdCliente())
                .nombreCliente(pedido.getCliente().getNombre())
                .fecha(pedido.getFecha())
                .total(pedido.getTotal())
                .items(items)
                .build();
    }
}

