package com.salesmaster.salesmasterpro.service;

import com.salesmaster.salesmasterpro.dto.ProductoDTO;
import com.salesmaster.salesmasterpro.entity.Producto;
import com.salesmaster.salesmasterpro.exception.ResourceNotFoundException;
import com.salesmaster.salesmasterpro.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public ProductoDTO crearProducto(ProductoDTO productoDTO) {
        Producto producto = Producto.builder()
                .nombre(productoDTO.getNombre())
                .precio(productoDTO.getPrecio())
                .build();

        Producto productoGuardado = productoRepository.save(producto);
        Long idOriginal = productoGuardado.getIdProd();
        
        // Reordenar los IDs después de crear para mantener secuencia (1, 2, 3, ...)
        reordenarIdsProductos();
        
        // Recargar el producto recién creado
        // Buscar por nombre y precio ya que el ID pudo haber cambiado
        Producto productoReordenado = productoRepository.findAll().stream()
                .filter(p -> p.getNombre().equals(productoDTO.getNombre()) && 
                            p.getPrecio().equals(productoDTO.getPrecio()))
                .findFirst()
                .orElse(productoRepository.findById(idOriginal)
                        .orElse(productoGuardado));
        
        return convertirADTO(productoReordenado);
    }

    public ProductoDTO actualizarProducto(Long id, ProductoDTO productoDTO) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        producto.setNombre(productoDTO.getNombre());
        producto.setPrecio(productoDTO.getPrecio());

        Producto productoActualizado = productoRepository.save(producto);
        return convertirADTO(productoActualizado);
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> listarProductos() {
        return productoRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductoDTO obtenerProductoPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        return convertirADTO(producto);
    }

    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        
        if (!producto.getPedidoProductos().isEmpty()) {
            throw new IllegalArgumentException(
                "No se puede eliminar el producto porque está incluido en pedidos. " +
                "No se pueden eliminar productos que ya están en pedidos."
            );
        }
        
        // Eliminar el producto
        productoRepository.deleteById(id);
        
        // Reordenar los IDs de los productos restantes
        reordenarIdsProductos();
    }
    
    /**
     * Reordena los IDs de los productos para que sean secuenciales (1, 2, 3, ...)
     * y actualiza las referencias en la tabla de pedido_producto usando SQL nativo
     */
    private void reordenarIdsProductos() {
        // Obtener todos los productos ordenados por ID actual
        List<Producto> productos = productoRepository.findAll().stream()
                .sorted((p1, p2) -> Long.compare(p1.getIdProd(), p2.getIdProd()))
                .collect(Collectors.toList());
        
        if (productos.isEmpty()) {
            // Resetear la secuencia si no hay productos
            entityManager.createNativeQuery(
                "SELECT setval('salesmaster.producto_id_prod_seq', 0, true)"
            ).getSingleResult();
            return;
        }
        
        // Usar IDs temporales altos para evitar conflictos
        Long idTemporalBase = 1000000L;
        
        try {
            // Paso 1: Eliminar temporalmente la foreign key constraint
            entityManager.createNativeQuery(
                "ALTER TABLE salesmaster.pedido_producto DROP CONSTRAINT IF EXISTS pedido_producto_id_prod_fkey"
            ).executeUpdate();
            
            // Paso 2: Asignar IDs temporales a todos los productos
            for (Producto producto : productos) {
                Long idAntiguo = producto.getIdProd();
                Long idTemporal = idTemporalBase + idAntiguo;
                
                // Actualizar ID del producto
                entityManager.createNativeQuery(
                    "UPDATE salesmaster.producto SET id_prod = :idTemporal WHERE id_prod = :idAntiguo"
                )
                .setParameter("idTemporal", idTemporal)
                .setParameter("idAntiguo", idAntiguo)
                .executeUpdate();
                
                // Actualizar referencias en pedido_producto
                entityManager.createNativeQuery(
                    "UPDATE salesmaster.pedido_producto SET id_prod = :idTemporal WHERE id_prod = :idAntiguo"
                )
                .setParameter("idTemporal", idTemporal)
                .setParameter("idAntiguo", idAntiguo)
                .executeUpdate();
            }
            
            // Limpiar el contexto de persistencia
            entityManager.clear();
            
            // Paso 3: Asignar IDs secuenciales finales (1, 2, 3, ...)
            @SuppressWarnings("unchecked")
            List<Number> productosTemporales = entityManager.createNativeQuery(
                "SELECT id_prod FROM salesmaster.producto WHERE id_prod >= :base ORDER BY id_prod"
            )
            .setParameter("base", idTemporalBase)
            .getResultList();
            
            for (int i = 0; i < productosTemporales.size(); i++) {
                Long idTemporal = productosTemporales.get(i).longValue();
                Long idNuevo = (long) (i + 1);
                
                // Actualizar ID del producto
                entityManager.createNativeQuery(
                    "UPDATE salesmaster.producto SET id_prod = :idNuevo WHERE id_prod = :idTemporal"
                )
                .setParameter("idNuevo", idNuevo)
                .setParameter("idTemporal", idTemporal)
                .executeUpdate();
                
                // Actualizar referencias en pedido_producto
                entityManager.createNativeQuery(
                    "UPDATE salesmaster.pedido_producto SET id_prod = :idNuevo WHERE id_prod = :idTemporal"
                )
                .setParameter("idNuevo", idNuevo)
                .setParameter("idTemporal", idTemporal)
                .executeUpdate();
            }
            
            // Paso 4: Recrear la foreign key constraint
            entityManager.createNativeQuery(
                "ALTER TABLE salesmaster.pedido_producto " +
                "ADD CONSTRAINT pedido_producto_id_prod_fkey " +
                "FOREIGN KEY (id_prod) REFERENCES salesmaster.producto(id_prod)"
            ).executeUpdate();
            
            // Paso 5: Resetear la secuencia de PostgreSQL
            Long maxId = (long) productosTemporales.size();
            entityManager.createNativeQuery(
                "SELECT setval('salesmaster.producto_id_prod_seq', :maxId, true)"
            )
            .setParameter("maxId", maxId)
            .getSingleResult();
            
        } catch (Exception e) {
            // En caso de error, intentar recrear la constraint si no existe
            try {
                entityManager.createNativeQuery(
                    "ALTER TABLE salesmaster.pedido_producto " +
                    "ADD CONSTRAINT pedido_producto_id_prod_fkey " +
                    "FOREIGN KEY (id_prod) REFERENCES salesmaster.producto(id_prod)"
                ).executeUpdate();
            } catch (Exception ex) {
                // La constraint ya existe, ignorar
            }
            throw new RuntimeException("Error al reordenar IDs de productos: " + e.getMessage(), e);
        } finally {
            // Limpiar el contexto
            entityManager.clear();
        }
    }

    private ProductoDTO convertirADTO(Producto producto) {
        return ProductoDTO.builder()
                .idProd(producto.getIdProd())
                .nombre(producto.getNombre())
                .precio(producto.getPrecio())
                .build();
    }
}

