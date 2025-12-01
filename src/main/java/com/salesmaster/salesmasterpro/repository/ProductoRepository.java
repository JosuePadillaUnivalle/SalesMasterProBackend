package com.salesmaster.salesmasterpro.repository;

import com.salesmaster.salesmasterpro.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
}

