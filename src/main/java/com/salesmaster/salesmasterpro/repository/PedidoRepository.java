package com.salesmaster.salesmasterpro.repository;

import com.salesmaster.salesmasterpro.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteIdCliente(Long idCliente);
}

