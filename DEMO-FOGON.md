# Demo Fogón — 12 minutos

Caballo de Troya: mostrador + stock + pedidos (WhatsApp/delivery/retiro) + mesas de salón.
No es un clon de Fudo: el mozo toma el pedido; el sistema controla mesas y cuentas.

Ramas: `feat/fogon-pedidos-demo` en API y web. **No mergear a main.**

## Crear el tenant (SuperAdmin)

El dueño carga esto a mano. No hay passwords en git.

1. Login SuperAdmin → **Nueva Empresa** → preset **Fogón** (o Aplicar Fogón en un tenant existente)
2. Configurar:
   - tenant: `fogon`
   - nombre: Fogón
   - IVA 21, efectivo ON
   - **remitos OFF**, stock/compras ON (compras quedan ocultas en el menú Fogón)
3. Features: **WHATSAPP_BOT** + **MESAS**
4. Login admin `fogon` → abrir caja → carta (~12 productos) → Mesas lote 1–12

## Menú que debe ver Fogón (ADMIN)

- Dashboard (Sala & cocina)
- POS
- **Cocina / Pedidos**
- Mesas
- Carta / Categorías
- Historial de ventas
- Usuarios / Config (acá se da de alta Socio, Caja y Delivery con WhatsApp)

**No** debería ver: Hojas de Ruta, Proveedores, Compras, Movimientos, Auditoría, Directorio Clientes.

## Alta de Delivery (WhatsApp, no app)

Fogón no gestiona flota ni pide que el rider entre a Tulum.

1. Login Socio Fogón → **Equipo** → rol **Delivery** + **WhatsApp** (el número real).
2. Delivery escribe *hola* al WhatsApp de Fogón (una vez, para abrir el chat).
3. Entra un delivery → le llega aviso. Cocina marca **Listo** → dirección, Maps, *tomo* / *entregado*.

Si no hay Delivery, el pedido queda en Listo. La parrilla no se rompe.

Cargá también WhatsApp de **Socio** (ADMIN) y **Caja** (OPERADOR) para los paneles.

PedidosYa queda como canal futuro. No está en esta demo.

## Guion de 12 minutos

1. Login `fogon` (aterriza en Cocina / Pedidos).
2. Abrir caja si hace falta.
3. Dashboard: métricas cocina / listos para salir / en camino / mesas.
4. POS: venta mostrador → baja stock.
5. Mesas: libre → abrir cuenta (ocupada + tiempo). Ítems en mesa = próximo corte.
6. Pedidos: pedido delivery a mano o **WhatsApp con dirección** → cocina hasta **Listo**.
7. Delivery recibe el WhatsApp (bot corriendo) → **Tomo** → Maps → **Entregado**.
8. Socio escribe *caja* / *mesas* / *stock*. Caja escribe *mesas* (no ve ventas del día).
9. Cocina: chip **Salida** muestra quién salió; no asigna delivery a mano.

## WhatsApp real (Meta Cloud API)

Adapter en `tulum-whatsapp-bot/` (no va dentro del JAR).

1. Configurá Meta (número de prueba + allowlist) según `tulum-whatsapp-bot/README.md`.
2. API con `BOT_SHARED_SECRET` = el del bot.
3. `npm install && npm start` en `tulum-whatsapp-bot` + ngrok al `/webhook`.
4. Escribí *hola* al número de prueba → pedido → aparece en Cocina.

Probar roles sin Meta (bot en 3099). Cuatro números distintos, o `/debug/mensaje` con esos `from`:

```bash
curl "http://localhost:3099/debug/rol?from=54911SOCIO"
curl -X POST http://localhost:3099/debug/mensaje -H "Content-Type: application/json" \
  -d '{"from":"54911SOCIO","text":"caja"}'
curl -X POST http://localhost:3099/debug/mensaje -H "Content-Type: application/json" \
  -d '{"from":"54911CAJA","text":"mesas"}'
curl -X POST http://localhost:3099/debug/mensaje -H "Content-Type: application/json" \
  -d '{"from":"54911CAJA","text":"vendimos"}'
curl -X POST http://localhost:3099/debug/mensaje -H "Content-Type: application/json" \
  -d '{"from":"54911DELIV","text":"hola"}'
curl -X POST http://localhost:3099/debug/mensaje -H "Content-Type: application/json" \
  -d '{"from":"5491199990000","text":"carta"}'
```

Checklist: cliente pide sánguche + Coca → cocina; Socio ve caja/mesas/stock; Caja ve mesas y **no** ventas del día; Delivery toma LISTO. QR o debug alcanzan (sin plantillas Meta).


Curl de respaldo (sin Meta):

```bash
curl -X POST http://localhost:8080/api/external/bot/pedido \
  -H "Content-Type: application/json" \
  -H "X-Bot-Secret: <secreto>" \
  -H "X-Tenant-ID: fogon" \
  -d '{
    "clienteTelefono": "54911...",
    "nombre": "Cliente demo",
    "direccion": "Av. Siempre Viva 123",
    "observaciones": "sin mayo",
    "metodoPago": "EFECTIVO",
    "items": [{ "productoId": 1, "cantidad": 2 }]
  }'
```

Con dirección, WhatsApp entra a cocina y **Delivery lo toma por chat** (Listo → Tomo → En camino → Entregado). Sin dirección queda como retiro (Listo → Entregado en local).

## Qué decirle a Fogón

- Hoy: stock, caja, mostrador, cocina, mesas, y delivery por WhatsApp (el rider no entra a Tulum). Socio y Caja consultan la operación por el mismo bot.
- Canal propio por WhatsApp (no PedidosYa). Delivery se maneja por chat; Fogón cocina.
- No prometan app de mozo, QR de mesa ni PedidosYa en este corte.
