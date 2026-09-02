package com.tulumcore.api.controllers;

import com.tulumcore.api.services.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/store/{tenantSlug}")
public class PublicStoreController {

    @Autowired
    private StoreService storeService;

    @GetMapping
    public ResponseEntity<StorefrontDTO> catalogo(@PathVariable String tenantSlug) {
        return ResponseEntity.ok(storeService.abrirCatalogo(tenantSlug));
    }

    @PostMapping("/pedidos")
    public ResponseEntity<PedidoCreadoDTO> crearPedido(
            @PathVariable String tenantSlug,
            @RequestBody StorePedidoRequest request
    ) {
        return ResponseEntity.ok(storeService.crearPedido(tenantSlug, request));
    }
}
