# Consultorio — demo Tulum

Vertical de agenda sobre el mismo JAR que Fogón y Chirino. No es historia clínica.

## Cómo crear el tenant demo

1. Entrar como SuperAdmin.
2. **Nueva empresa** → **Preset Consultorio**.
3. Slug: `consultorio`. Nombre: el del médico o “Consultorio demo”.
4. Admin inicial (email + clave de 8+ caracteres).
5. Crear. Si el tenant ya existe, abrirlo y **Aplicar Consultorio**.

El paquete deja:

- `AGENDA` y `WHATSAPP_BOT` encendidos
- Mesas y código de barras apagados
- Remitos, compras y stock apagados
- Pacientes (clientes) encendidos
- Horario 9–18, turnos de 20 min, lunes a viernes

## Datos para mostrar

En **Prácticas** crear al menos:

- Consulta (precio particular)
- Control (opcional)

En **Equipo**:

- ADMIN = Médico (WhatsApp del profesional)
- OPERADOR = Secretaria (WhatsApp de recepción)

El bot usa `TENANT_ID` del `.env`. Para este demo: `TENANT_ID=consultorio` (Fogón sigue siendo `fogon` cuando demos la parrilla).

## Qué probar

- Secretaria: Agenda → nuevo turno → confirmar / en espera / atendido.
- Cobrar efectivo o transferencia (caja abierta).
- Paciente por WhatsApp: pide turno, confirma, recibe recordatorio.
- Médico por WhatsApp: agenda del día, próximo, atendido, no vino.

## Qué no entra en este corte

Historia clínica, receta electrónica, PAMI, teleconsulta.
