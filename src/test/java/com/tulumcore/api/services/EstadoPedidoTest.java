package com.tulumcore.api.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoPedidoTest {

    @Test
    void envioListoLoDespachaLaCocina() {
        assertEquals(EstadoPedido.EN_CAMINO,
                EstadoPedido.siguientes(EstadoPedido.LISTO, CanalVenta.DELIVERY, "Calle 123").get(0));
        assertEquals(EstadoPedido.EN_CAMINO,
                EstadoPedido.siguientes(EstadoPedido.LISTO, CanalVenta.WHATSAPP, "Calle 123").get(0));
    }

    @Test
    void retiroListoLoCierraLaCocina() {
        assertEquals(EstadoPedido.ENTREGADO,
                EstadoPedido.siguientes(EstadoPedido.LISTO, CanalVenta.RETIRO, null).get(0));
        assertEquals(EstadoPedido.ENTREGADO,
                EstadoPedido.siguientes(EstadoPedido.LISTO, CanalVenta.WHATSAPP, null).get(0));
    }

    @Test
    void envioEnCaminoSeMarcaEntregado() {
        assertEquals(EstadoPedido.ENTREGADO,
                EstadoPedido.siguientes(EstadoPedido.EN_CAMINO, CanalVenta.DELIVERY, "Calle 1").get(0));
    }

    @Test
    void salonListoNoCierraLaCuenta() {
        assertEquals(EstadoPedido.EN_PREPARACION,
                EstadoPedido.siguientes(EstadoPedido.PENDIENTE, CanalVenta.SALON, null).get(0));
        assertEquals(EstadoPedido.LISTO,
                EstadoPedido.siguientes(EstadoPedido.EN_PREPARACION, CanalVenta.SALON, null).get(0));
        assertTrue(EstadoPedido.siguientes(EstadoPedido.LISTO, CanalVenta.SALON, null).isEmpty());
    }
}

class CanalVentaCocinaTest {

    @Test
    void cocinaVeSalonYLosPedidosSiguenApartados() {
        assertTrue(CanalVenta.canalesCocina().contains(CanalVenta.SALON));
        assertFalse(CanalVenta.canalesPedido().contains(CanalVenta.SALON));
        assertTrue(CanalVenta.esCuentaAbierta(CanalVenta.SALON));
        assertFalse(CanalVenta.esPedido(CanalVenta.SALON));
    }
}

class PedidoSalidaTest {

    @Test
    void puedeTomarSoloListoSinCadete() {
        assertTrue(PedidoSalida.puedeTomar(EstadoPedido.LISTO, CanalVenta.DELIVERY, "Av. 1", null));
        assertFalse(PedidoSalida.puedeTomar(EstadoPedido.LISTO, CanalVenta.DELIVERY, "Av. 1", 9L));
        assertFalse(PedidoSalida.puedeTomar(EstadoPedido.EN_PREPARACION, CanalVenta.DELIVERY, "Av. 1", null));
        assertFalse(PedidoSalida.puedeTomar(EstadoPedido.LISTO, CanalVenta.RETIRO, null, null));
        assertFalse(PedidoSalida.puedeTomar(EstadoPedido.LISTO, CanalVenta.SALON, null, null));
    }

    @Test
    void puedeReservarEnvioEnCocina() {
        assertTrue(PedidoSalida.puedeReservar(EstadoPedido.PENDIENTE, CanalVenta.DELIVERY, "Av. 1", null));
        assertTrue(PedidoSalida.puedeReservar(EstadoPedido.EN_PREPARACION, CanalVenta.WHATSAPP, "Av. 1", null));
        assertFalse(PedidoSalida.puedeReservar(EstadoPedido.EN_PREPARACION, CanalVenta.DELIVERY, "Av. 1", 9L));
        assertFalse(PedidoSalida.puedeReservar(EstadoPedido.LISTO, CanalVenta.DELIVERY, "Av. 1", null));
    }

    @Test
    void puedeSalirListoPropioOLibre() {
        assertTrue(PedidoSalida.puedeSalir(EstadoPedido.LISTO, CanalVenta.DELIVERY, "Av. 1", null, 3L));
        assertTrue(PedidoSalida.puedeSalir(EstadoPedido.LISTO, CanalVenta.DELIVERY, "Av. 1", 3L, 3L));
        assertFalse(PedidoSalida.puedeSalir(EstadoPedido.LISTO, CanalVenta.DELIVERY, "Av. 1", 9L, 3L));
        assertFalse(PedidoSalida.puedeSalir(EstadoPedido.EN_PREPARACION, CanalVenta.DELIVERY, "Av. 1", null, 3L));
    }

    @Test
    void nombreVisibleUsaLaParteLocalDelEmail() {
        com.tulumcore.api.entities.Usuario u = new com.tulumcore.api.entities.Usuario();
        u.setEmail("juan.perez@fogon.com");
        assertEquals("juan perez", PedidoSalida.nombreVisible(u));
    }

    @Test
    void nombreVisibleDeCadeteSinEmailUsaElWhatsApp() {
        com.tulumcore.api.entities.Usuario u = new com.tulumcore.api.entities.Usuario();
        u.setEmail("cadete.5491112345678@fogon.tulum.local");
        u.setTelefono("5491112345678");
        assertEquals("Cadete 5678", PedidoSalida.nombreVisible(u));
    }

    @Test
    void nombreEquipoUsaEtiquetasFogon() {
        com.tulumcore.api.entities.Usuario socio = new com.tulumcore.api.entities.Usuario();
        socio.setEmail("socio.5491111111111@fogon.tulum.local");
        socio.setTelefono("5491111111111");
        socio.setRol(com.tulumcore.api.entities.Rol.ADMIN);
        assertEquals("Socio 1111", PedidoSalida.nombreEquipo(socio));

        com.tulumcore.api.entities.Usuario caja = new com.tulumcore.api.entities.Usuario();
        caja.setEmail("caja.5491122222222@fogon.tulum.local");
        caja.setTelefono("5491122222222");
        caja.setRol(com.tulumcore.api.entities.Rol.OPERADOR);
        assertEquals("Caja 2222", PedidoSalida.nombreEquipo(caja));

        com.tulumcore.api.entities.Usuario delivery = new com.tulumcore.api.entities.Usuario();
        delivery.setEmail("cadete.5491133333333@fogon.tulum.local");
        delivery.setTelefono("5491133333333");
        delivery.setRol(com.tulumcore.api.entities.Rol.REPARTIDOR);
        assertEquals("Delivery 3333", PedidoSalida.nombreEquipo(delivery));
    }
}

class TelefonoWhatsAppTest {

    @Test
    void normalizaArgentinaComoMeta() {
        assertEquals("5491112345678", TelefonoWhatsApp.normalizar("+54 9 11 1234-5678"));
        assertEquals("5491112345678", TelefonoWhatsApp.normalizar("11 1234-5678"));
        assertEquals("5491112345678", TelefonoWhatsApp.normalizar("5491112345678"));
        assertEquals("5492645859460", TelefonoWhatsApp.normalizar("2645859460"));
        assertEquals("5492645859460", TelefonoWhatsApp.normalizar("542645859460"));
        assertNull(TelefonoWhatsApp.normalizar("5470059556921490"));
    }

    @Test
    void mismaLineaIgnoraPrefijo() {
        assertTrue(TelefonoWhatsApp.mismaLinea("2646267476", "5492646267476"));
        assertFalse(TelefonoWhatsApp.mismaLinea("2646267476", "2645859460"));
    }
}

class DivisionCuentaTest {

    @Test
    void tresPartesRepartenLosCentavosEnLaUltima() {
        assertEquals(333.33, DivisionCuenta.parteActual(1000, 0, 3, 0), 0.001);
        assertEquals(333.33, DivisionCuenta.parteActual(1000, 333.33, 3, 1), 0.001);
        assertEquals(333.34, DivisionCuenta.parteActual(1000, 666.66, 3, 2), 0.001);
    }

    @Test
    void unMontoNoPasaElSaldo() {
        assertEquals(400, DivisionCuenta.cobrarMonto(400, 900), 0.001);
        assertEquals(150, DivisionCuenta.cobrarMonto(400, 150), 0.001);
    }

    @Test
    void cubreCuandoLasPartesLleganAlTotal() {
        assertTrue(DivisionCuenta.cubreElTotal(1000, 1000));
        assertFalse(DivisionCuenta.cubreElTotal(1000, 999.98));
    }
}
