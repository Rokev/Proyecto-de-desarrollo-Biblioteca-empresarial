# Registro de Decisiones de Arquitectura (ADR)

## ADR-001: Angular 22 Standalone + Zoneless + Signals
- **Contexto:** Sistema multi-rol detrás de login con alta densidad de formularios, tablas y flujos logísticos.
- **Decisión:** Implementar CSR (`--ssr=false`), modo `--zoneless` y reactividad basada en `signal`, `computed`, `effect` y `linkedSignal`.
- **Consecuencia:** Eliminación de sobrecarga de `zone.js`, actualizaciones granulares del DOM y acceso directo a `localStorage` para el JWT.

## ADR-002: Atomic Design Estricto en `shared/ui`
- **Contexto:** Múltiples roles comparten tarjetas, formularios, tablas, modales y flujos de pago.
- **Decisión:** Ningún átomo, molécula u organismo (salvo `Navbar`, `Sidebar` y `ChatbotWidget`) realiza peticiones HTTP; las *Pages* inyectan los servicios y pasan datos/eventos hacia abajo.

## ADR-003: Flujo de Pago en Dos Pasos Alineado al Backend
- **Contexto:** El backend crea primero una entidad en estado `PENDIENTE_PAGO` (suscripción, reserva de sala o pedido interciudad) o una multa `PENDIENTE`, y luego liquida mediante `POST /api/v1/pagos/procesar`.
- **Decisión:** Centralizar todos los cobros en la ruta `/pago/:tipo/:id` (`PaymentPage`), pasando el monto exacto por query param/estado y mostrando `ReceiptCard` con los últimos 4 dígitos.
