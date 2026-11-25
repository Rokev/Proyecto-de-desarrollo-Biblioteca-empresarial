# Observaciones de Backend Detectadas Durante Pruebas

1. **Edición de `nombreUsuario` en perfil (Pruebas 8 y 9):**
   - `ActualizarPerfilDTO` (`PUT /api/v1/auth/me`) solo recibe `nombre`, `apellido`, `telefono` y `direccion`, mientras que `PUT /api/v1/usuarios/{id}` sí valida y actualiza `nombreUsuario` único (pero está restringido a rol `ADMIN` en `SecurityConfig`).
   - *Manejo Frontend:* `ProfilePage` permite editar `nombreUsuario`, intenta `PUT /api/v1/usuarios/{id}` y captura el error de duplicado (`400`) o permiso (`403`) mostrándolo claramente en pantalla.

2. **Pago múltiple de multas en un único comprobante (Prueba 31):**
   - `SolicitudPagoDTO` (`POST /api/v1/pagos/procesar`) recibe un único `multaId` por llamada y valida que `monto == multa.monto`.
   - *Manejo Frontend:* `PaymentPage` procesa las multas seleccionadas en bloque, consolida la suma total pagada y presenta un comprobante unificado con la suma total y todas las referencias generadas.
