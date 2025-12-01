package com.salesmaster.salesmasterpro.repository;

import com.salesmaster.salesmasterpro.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    Optional<Factura> findByPedidoIdPedido(Long idPedido);
    long countByNroStartingWith(String prefijo);
}

