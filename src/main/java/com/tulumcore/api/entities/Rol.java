package com.tulumcore.api.entities;

public enum Rol {
    SUPER_ADMIN, // Acceso global: administración de todos los tenants
    ADMIN,       // Acceso total: configuración, usuarios, reportes, caja
    OPERADOR,    // Acceso operativo: ventas, productos, clientes
    PREVENTISTA  // Toma pedidos y remitos en la calle: sin punto de venta ni caja
}