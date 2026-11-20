# Biblioteca Red — Frontend Empresarial (Angular 22)

Aplicación web SPA desarrollada en **Angular 22** (Standalone, Zoneless, Signals, `OnPush` por defecto) bajo arquitectura **Atomic Design** para consumir la API REST Spring Boot (`/api/v1`) del Sistema de Gestión de Biblioteca Empresarial.

## Requisitos Previos
- **Node.js** 22.x o superior
- **Angular CLI** 22 (`npm install -g @angular/cli@22`)
- **Backend Spring Boot** ejecutándose en `http://localhost:8080`

## Instalación y Ejecución

```bash
# 1. Instalar dependencias
npm install

# 2. Iniciar servidor de desarrollo (http://localhost:4200)
npm start

# 3. Compilar para producción
npm run build

# 4. Ejecutar pruebas unitarias con Vitest
npm test
```

## Roles y Credenciales de Prueba
- **Cliente (sin o con suscripción):** Registro libre en `/registro` (`CLIENTE`).
- **Bibliotecario (`BIBLIOTECARIO`), Repartidor (`REPARTIDOR`), Administrador (`ADMIN`):** Creados por el Administrador en `/admin/usuarios`.
- **Tarjetas de prueba simuladas (Pasarela `/pago/:tipo/:id`):**
  - Aprobadas: `4111111111111111`, `5555555555554444`
  - Rechazadas: `4000000000000002`, `4000000000009995`

## Estructura de Arquitectura
- `src/app/core/`: Modelos TypeScript alineados a DTOs, servicios singleton con Signals, guards de rol/suscripción e interceptores funcionales (`registro`, `carga`, `auth`, `errores`).
- `src/app/shared/ui/`: Componentes puros organizados por **Atomic Design** (`atoms`, `molecules`, `organisms`, `templates`).
- `src/app/features/`: Páginas inteligentes organizadas por dominio de negocio con *Lazy Loading*.
