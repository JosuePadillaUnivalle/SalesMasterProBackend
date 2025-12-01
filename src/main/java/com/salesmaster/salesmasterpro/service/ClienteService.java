package com.salesmaster.salesmasterpro.service;

import com.salesmaster.salesmasterpro.dto.ClienteDTO;
import com.salesmaster.salesmasterpro.entity.Cliente;
import com.salesmaster.salesmasterpro.exception.ResourceNotFoundException;
import com.salesmaster.salesmasterpro.repository.ClienteRepository;
import com.salesmaster.salesmasterpro.repository.PedidoRepository;
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
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public ClienteDTO crearCliente(ClienteDTO clienteDTO) {
        if (clienteRepository.findByEmail(clienteDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un cliente con el email: " + clienteDTO.getEmail());
        }

        Cliente cliente = Cliente.builder()
                .nombre(clienteDTO.getNombre())
                .email(clienteDTO.getEmail())
                .build();

        Cliente clienteGuardado = clienteRepository.save(cliente);
        
        // Reordenar los IDs después de crear para mantener secuencia (1, 2, 3, ...)
        reordenarIdsClientes();
        
        // Recargar el cliente recién creado por email (ya que el ID pudo haber cambiado)
        Cliente clienteReordenado = clienteRepository.findByEmail(clienteDTO.getEmail())
                .orElse(clienteGuardado);
        
        return convertirADTO(clienteReordenado);
    }

    public ClienteDTO actualizarCliente(Long id, ClienteDTO clienteDTO) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        if (!cliente.getEmail().equals(clienteDTO.getEmail())) {
            if (clienteRepository.findByEmail(clienteDTO.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Ya existe un cliente con el email: " + clienteDTO.getEmail());
            }
        }

        cliente.setNombre(clienteDTO.getNombre());
        cliente.setEmail(clienteDTO.getEmail());

        Cliente clienteActualizado = clienteRepository.save(cliente);
        return convertirADTO(clienteActualizado);
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO> listarClientes() {
        return clienteRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteDTO obtenerClientePorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        return convertirADTO(cliente);
    }

    public void eliminarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        
        if (!cliente.getPedidos().isEmpty()) {
            throw new IllegalArgumentException(
                "No se puede eliminar el cliente porque tiene pedidos asociados. " +
                "Elimine primero los pedidos relacionados."
            );
        }
        
        // Eliminar el cliente
        clienteRepository.deleteById(id);
        
        // Reordenar los IDs de los clientes restantes
        reordenarIdsClientes();
    }
    
    /**
     * Reordena los IDs de los clientes para que sean secuenciales (1, 2, 3, ...)
     * y actualiza las referencias en la tabla de pedidos usando SQL nativo
     */
    private void reordenarIdsClientes() {
        // Obtener todos los clientes ordenados por ID actual
        List<Cliente> clientes = clienteRepository.findAll().stream()
                .sorted((c1, c2) -> Long.compare(c1.getIdCliente(), c2.getIdCliente()))
                .collect(Collectors.toList());
        
        if (clientes.isEmpty()) {
            // Resetear la secuencia si no hay clientes
            entityManager.createNativeQuery(
                "SELECT setval('salesmaster.cliente_id_cliente_seq', 0, true)"
            ).getSingleResult();
            return;
        }
        
        // Usar IDs temporales altos para evitar conflictos
        Long idTemporalBase = 1000000L;
        
        try {
            // Paso 1: Eliminar temporalmente la foreign key constraint
            entityManager.createNativeQuery(
                "ALTER TABLE salesmaster.pedido DROP CONSTRAINT IF EXISTS pedido_id_cliente_fkey"
            ).executeUpdate();
            
            // Paso 2: Asignar IDs temporales a todos los clientes
            for (Cliente cliente : clientes) {
                Long idAntiguo = cliente.getIdCliente();
                Long idTemporal = idTemporalBase + idAntiguo;
                
                // Actualizar ID del cliente
                entityManager.createNativeQuery(
                    "UPDATE salesmaster.cliente SET id_cliente = :idTemporal WHERE id_cliente = :idAntiguo"
                )
                .setParameter("idTemporal", idTemporal)
                .setParameter("idAntiguo", idAntiguo)
                .executeUpdate();
                
                // Actualizar referencias en pedidos
                entityManager.createNativeQuery(
                    "UPDATE salesmaster.pedido SET id_cliente = :idTemporal WHERE id_cliente = :idAntiguo"
                )
                .setParameter("idTemporal", idTemporal)
                .setParameter("idAntiguo", idAntiguo)
                .executeUpdate();
            }
            
            // Limpiar el contexto de persistencia
            entityManager.clear();
            
            // Paso 3: Asignar IDs secuenciales finales (1, 2, 3, ...)
            @SuppressWarnings("unchecked")
            List<Number> clientesTemporales = entityManager.createNativeQuery(
                "SELECT id_cliente FROM salesmaster.cliente WHERE id_cliente >= :base ORDER BY id_cliente"
            )
            .setParameter("base", idTemporalBase)
            .getResultList();
            
            for (int i = 0; i < clientesTemporales.size(); i++) {
                Long idTemporal = clientesTemporales.get(i).longValue();
                Long idNuevo = (long) (i + 1);
                
                // Actualizar ID del cliente
                entityManager.createNativeQuery(
                    "UPDATE salesmaster.cliente SET id_cliente = :idNuevo WHERE id_cliente = :idTemporal"
                )
                .setParameter("idNuevo", idNuevo)
                .setParameter("idTemporal", idTemporal)
                .executeUpdate();
                
                // Actualizar referencias en pedidos
                entityManager.createNativeQuery(
                    "UPDATE salesmaster.pedido SET id_cliente = :idNuevo WHERE id_cliente = :idTemporal"
                )
                .setParameter("idNuevo", idNuevo)
                .setParameter("idTemporal", idTemporal)
                .executeUpdate();
            }
            
            // Paso 4: Recrear la foreign key constraint
            entityManager.createNativeQuery(
                "ALTER TABLE salesmaster.pedido " +
                "ADD CONSTRAINT pedido_id_cliente_fkey " +
                "FOREIGN KEY (id_cliente) REFERENCES salesmaster.cliente(id_cliente)"
            ).executeUpdate();
            
            // Paso 5: Resetear la secuencia de PostgreSQL
            Long maxId = (long) clientesTemporales.size();
            entityManager.createNativeQuery(
                "SELECT setval('salesmaster.cliente_id_cliente_seq', :maxId, true)"
            )
            .setParameter("maxId", maxId)
            .getSingleResult();
            
        } catch (Exception e) {
            // En caso de error, intentar recrear la constraint si no existe
            try {
                entityManager.createNativeQuery(
                    "ALTER TABLE salesmaster.pedido " +
                    "ADD CONSTRAINT pedido_id_cliente_fkey " +
                    "FOREIGN KEY (id_cliente) REFERENCES salesmaster.cliente(id_cliente)"
                ).executeUpdate();
            } catch (Exception ex) {
                // La constraint ya existe, ignorar
            }
            throw new RuntimeException("Error al reordenar IDs de clientes: " + e.getMessage(), e);
        } finally {
            // Limpiar el contexto
            entityManager.clear();
        }
    }

    private ClienteDTO convertirADTO(Cliente cliente) {
        return ClienteDTO.builder()
                .idCliente(cliente.getIdCliente())
                .nombre(cliente.getNombre())
                .email(cliente.getEmail())
                .build();
    }
}

