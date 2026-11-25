import { Routes } from '@angular/router';
import { authGuard, roleGuard, subscriptionGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'catalogo', pathMatch: 'full' },

  // Autenticación
  { path: 'login', title: 'Iniciar sesión', loadComponent: () => import('./features/auth/login-page').then(m => m.LoginPage) },
  { path: 'registro', title: 'Registro', loadComponent: () => import('./features/auth/register-page').then(m => m.RegisterPage) },
  { path: 'recuperar', title: 'Recuperar contraseña', loadComponent: () => import('./features/auth/recover-password-page').then(m => m.RecoverPasswordPage) },
  { path: 'recuperar/:token', title: 'Restablecer contraseña', loadComponent: () => import('./features/auth/recover-password-page').then(m => m.RecoverPasswordPage) },

  // Catálogo y Sedes
  { path: 'catalogo', title: 'Catálogo', loadComponent: () => import('./features/catalogo/catalog-page').then(m => m.CatalogPage) },
  { path: 'catalogo/:libroId', title: 'Detalle de libro', loadComponent: () => import('./features/catalogo/book-detail-page').then(m => m.BookDetailPage) },
  { path: 'catalogo/:libroId/solicitar', canActivate: [authGuard], title: 'Solicitar préstamo', loadComponent: () => import('./features/prestamos/loan-request-page').then(m => m.LoanRequestPage) },
  { path: 'sedes', title: 'Bibliotecas', loadComponent: () => import('./features/catalogo/sedes-page').then(m => m.SedesPage) },
  { path: 'sedes/:sedeId/salas', title: 'Salas de estudio', loadComponent: () => import('./features/salas/rooms-page').then(m => m.RoomsPage) },

  // Eventos y Planes
  { path: 'eventos', title: 'Eventos', loadComponent: () => import('./features/eventos/events-page').then(m => m.EventsPage) },
  { path: 'eventos/:eventoId', title: 'Detalle de evento', loadComponent: () => import('./features/eventos/event-detail-page').then(m => m.EventDetailPage) },
  { path: 'planes', title: 'Suscripciones', loadComponent: () => import('./features/cuenta-pagos/plans-page').then(m => m.PlansPage) },

  // Área de Usuario
  { path: 'mi-cuenta/prestamos', canActivate: [authGuard], title: 'Mis préstamos y reservas', loadComponent: () => import('./features/prestamos/my-loans-page').then(m => m.MyLoansPage) },
  { path: 'mi-cuenta/prestamos/:prestamoId/devolver', canActivate: [authGuard], title: 'Devolver libro', loadComponent: () => import('./features/prestamos/return-request-page').then(m => m.ReturnRequestPage) },
  { path: 'mi-cuenta/reservas', canActivate: [authGuard], title: 'Mis reservas', loadComponent: () => import('./features/prestamos/my-reservations-page').then(m => m.MyReservationsPage) },
  { path: 'mi-cuenta/salas', canActivate: [authGuard], title: 'Mis salas', loadComponent: () => import('./features/salas/my-room-reservations-page').then(m => m.MyRoomReservationsPage) },
  { path: 'mi-cuenta/eventos', canActivate: [authGuard], title: 'Mis eventos', loadComponent: () => import('./features/eventos/my-events-page').then(m => m.MyEventsPage) },
  { path: 'mi-cuenta/multas', canActivate: [authGuard], title: 'Multas y sanciones', loadComponent: () => import('./features/cuenta-pagos/fines-page').then(m => m.FinesPage) },
  { path: 'mi-cuenta/pagos', canActivate: [authGuard], title: 'Historial de pagos', loadComponent: () => import('./features/cuenta-pagos/payment-history-page').then(m => m.PaymentHistoryPage) },
  { path: 'mi-cuenta/suscripcion', canActivate: [authGuard], title: 'Mi suscripción', loadComponent: () => import('./features/cuenta-pagos/my-subscription-page').then(m => m.MySubscriptionPage) },
  { path: 'mi-cuenta/perfil', canActivate: [authGuard], title: 'Mi perfil', loadComponent: () => import('./features/cuenta-pagos/profile-page').then(m => m.ProfilePage) },
  { path: 'notificaciones', canActivate: [authGuard], title: 'Notificaciones', loadComponent: () => import('./features/cuenta-pagos/notifications-page').then(m => m.NotificationsPage) },
  { path: 'pago/:tipo/:id', canActivate: [authGuard], title: 'Pasarela de pago', loadComponent: () => import('./features/cuenta-pagos/payment-page').then(m => m.PaymentPage) },

  // Pedidos Interciudad
  { path: 'pedidos/nuevo', canActivate: [subscriptionGuard], title: 'Nuevo pedido interciudad', loadComponent: () => import('./features/pedidos-interciudad/intercity-order-page').then(m => m.InterCityOrderPage) },
  { path: 'mi-cuenta/pedidos', canActivate: [subscriptionGuard], title: 'Mis pedidos interciudad', loadComponent: () => import('./features/pedidos-interciudad/intercity-orders-page').then(m => m.InterCityOrdersPage) },
  { path: 'mi-cuenta/pedidos/:pedidoId', canActivate: [subscriptionGuard], title: 'Detalle de pedido', loadComponent: () => import('./features/pedidos-interciudad/intercity-order-detail-page').then(m => m.InterCityOrderDetailPage) },

  // Panel Bibliotecario
  { path: 'panel', canActivate: [roleGuard(['BIBLIOTECARIO', 'ADMIN'])], title: 'Panel Sede', loadComponent: () => import('./features/panel-bibliotecario/staff-dashboard-page').then(m => m.StaffDashboardPage) },
  { path: 'panel/prestamos', canActivate: [roleGuard(['BIBLIOTECARIO', 'ADMIN'])], title: 'Gestión Préstamos', loadComponent: () => import('./features/panel-bibliotecario/staff-operations-page').then(m => m.StaffOperationsPage) },
  { path: 'panel/prestamos/:id', canActivate: [roleGuard(['BIBLIOTECARIO', 'ADMIN'])], title: 'Detalle de Préstamo', loadComponent: () => import('./features/panel-bibliotecario/staff-loan-detail-page').then(m => m.StaffLoanDetailPage) },
  { path: 'panel/reservas', canActivate: [roleGuard(['BIBLIOTECARIO', 'ADMIN'])], title: 'Gestión Reservas', loadComponent: () => import('./features/panel-bibliotecario/staff-reservations-page').then(m => m.StaffReservationsPage) },
  { path: 'panel/devoluciones', canActivate: [roleGuard(['BIBLIOTECARIO', 'ADMIN'])], title: 'Confirmar Devolución', loadComponent: () => import('./features/panel-bibliotecario/return-confirmation-page').then(m => m.ReturnConfirmationPage) },
  { path: 'panel/libros', canActivate: [roleGuard(['BIBLIOTECARIO', 'ADMIN'])], title: 'Gestión Libros, Autores y Géneros', loadComponent: () => import('./features/panel-bibliotecario/staff-catalog-page').then(m => m.StaffCatalogPage) },
  { path: 'panel/ejemplares', canActivate: [roleGuard(['BIBLIOTECARIO', 'ADMIN'])], title: 'Ejemplares y Stock', loadComponent: () => import('./features/panel-bibliotecario/staff-copies-page').then(m => m.StaffCopiesPage) },
  { path: 'panel/salas', canActivate: [roleGuard(['BIBLIOTECARIO', 'ADMIN'])], title: 'Gestión Salas', loadComponent: () => import('./features/panel-bibliotecario/staff-rooms-page').then(m => m.StaffRoomsPage) },
  { path: 'panel/eventos', canActivate: [roleGuard(['BIBLIOTECARIO', 'ADMIN'])], title: 'Gestión Eventos', loadComponent: () => import('./features/panel-bibliotecario/staff-events-page').then(m => m.StaffEventsPage) },

  // Panel Repartidor
  { path: 'reparto', canActivate: [roleGuard(['REPARTIDOR'])], title: 'Tablero Repartidor', loadComponent: () => import('./features/panel-repartidor/delivery-board-page').then(m => m.DeliveryBoardPage) },

  // Panel Administrador
  { path: 'admin/usuarios', canActivate: [roleGuard(['ADMIN'])], title: 'Usuarios y Personal', loadComponent: () => import('./features/panel-admin/admin-users-page').then(m => m.AdminUsersPage) },
  { path: 'admin/sedes', canActivate: [roleGuard(['ADMIN'])], title: 'Sedes', loadComponent: () => import('./features/panel-admin/admin-sedes-page').then(m => m.AdminSedesPage) },
  { path: 'admin/planes', canActivate: [roleGuard(['ADMIN'])], title: 'Planes', loadComponent: () => import('./features/panel-admin/admin-operations-pages').then(m => m.AdminPlansPage) },
  { path: 'admin/politicas', canActivate: [roleGuard(['ADMIN'])], title: 'Políticas y Plazos', loadComponent: () => import('./features/panel-admin/admin-operations-pages').then(m => m.AdminPoliciesPage) },
  { path: 'admin/tarifas-envio', canActivate: [roleGuard(['ADMIN'])], title: 'Tarifas de Envío', loadComponent: () => import('./features/panel-admin/admin-operations-pages').then(m => m.AdminShippingPage) },
  { path: 'admin/pagos', canActivate: [roleGuard(['ADMIN'])], title: 'Pagos', loadComponent: () => import('./features/panel-admin/admin-operations-pages').then(m => m.AdminPaymentsPage) },
  { path: 'admin/reportes', canActivate: [roleGuard(['ADMIN'])], title: 'Reportes', loadComponent: () => import('./features/panel-admin/admin-operations-pages').then(m => m.AdminReportsPage) },
  { path: 'admin/auditoria', canActivate: [roleGuard(['ADMIN'])], title: 'Auditoría', loadComponent: () => import('./features/panel-admin/admin-operations-pages').then(m => m.AdminAuditPage) },
  { path: 'admin/faq', canActivate: [roleGuard(['ADMIN'])], title: 'Chatbot FAQ', loadComponent: () => import('./features/panel-admin/admin-operations-pages').then(m => m.AdminFaqPage) },

  // Errores
  { path: 'acceso-denegado', title: 'Acceso denegado', loadComponent: () => import('./features/errores/not-found-page').then(m => m.NotFoundPage) },
  { path: '**', title: 'No encontrado', loadComponent: () => import('./features/errores/not-found-page').then(m => m.NotFoundPage) },
];
