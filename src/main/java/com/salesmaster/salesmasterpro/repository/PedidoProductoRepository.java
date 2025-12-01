package com.salesmaster.salesmasterpro.repository;

import com.salesmaster.salesmasterpro.entity.PedidoProducto;
import com.salesmaster.salesmasterpro.entity.PedidoProductoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoProductoRepository extends JpaRepository<PedidoProducto, PedidoProductoId> {
    List<PedidoProducto> findByIdIdPedido(Long idPedido);
}

