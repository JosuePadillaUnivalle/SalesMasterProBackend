package com.salesmaster.salesmasterpro.controller;

import com.salesmaster.salesmasterpro.dto.FacturaDTO;
import com.salesmaster.salesmasterpro.service.FacturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
@Tag(name = "Facturas", description = "API para gestión de facturas")
public class FacturaController {

    private final FacturaService facturaService;

    @PostMapping("/{idPedido}")
    @Operation(summary = "Generar factura", description = "Genera una factura basada en un pedido existente")
    public ResponseEntity<FacturaDTO> generarFactura(@PathVariable Long idPedido) {
        FacturaDTO facturaCreada = facturaService.generarFactura(idPedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaCreada);
    }

    @GetMapping
    @Operation(summary = "Listar todas las facturas", description = "Obtiene una lista de todas las facturas registradas")
    public ResponseEntity<List<FacturaDTO>> listarFacturas() {
        List<FacturaDTO> facturas = facturaService.listarFacturas();
        return ResponseEntity.ok(facturas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener factura por ID", description = "Obtiene los detalles de una factura específica")
    public ResponseEntity<FacturaDTO> obtenerFactura(@PathVariable Long id) {
        FacturaDTO factura = facturaService.obtenerFacturaPorId(id);
        return ResponseEntity.ok(factura);
    }
}

