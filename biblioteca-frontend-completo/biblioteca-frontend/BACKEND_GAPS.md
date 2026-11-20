# Reporte de Brechas e Inconsistencias del Backend (`BACKEND_GAPS.md`)

1. **Justificación formal de retraso vía Chatbot (HU-15):**
   - *Observación:* `ChatbotController` expone `POST /api/v1/chatbot/mensaje` (`ChatbotMensajeDTO`), pero no existe un endpoint dedicado para radicar una justificación formal asociada a un `prestamoId` con notificación directa al bibliotecario.
   - *Solución Frontend:* `ChatbotWidget` incorpora `DelayJustificationForm`, enviando la justificación formateada al endpoint `/api/v1/chatbot/mensaje` y mostrando confirmación al usuario.

2. **Pago múltiple de multas en una sola transacción (RF-15 / Caso de Prueba 31):**
   - *Observación:* `SolicitudPagoDTO` recibe un único `Long multaId` por invocación en `POST /api/v1/pagos/procesar`.
   - *Solución Frontend:* `FinesPage` permite seleccionar múltiples multas con `FineItem` (`Checkbox`), suma el total en tiempo real y procesa secuencialmente cada multa seleccionada contra `/api/v1/pagos/procesar`, consolidando los comprobantes emitidos.

3. **Obtención de datos completos del usuario tras Login (`AuthResponseDTO`):**
   - *Observación:* `POST /api/v1/auth/login` retorna `{ token, tipo, expiracion, usuarioId, nombre, rol }`, sin incluir `sedeId` o `correo`.
   - *Solución Frontend:* Inmediatamente tras recibir el token en `AuthService.login()`, se consulta `GET /api/v1/auth/me` para hidratar el `UsuarioDTO` completo (incluyendo `sedeId` para bibliotecarios) y `GET /api/v1/suscripciones/usuario/{id}/vigente` para clientes.
