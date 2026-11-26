import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AsistenteEventoDTO,
  ComprobantePagoDTO,
  DevolucionDTO,
  DisponibilidadSalasDTO,
  EventoDTO,
  InscripcionEventoDTO,
  MultaDTO,
  NotificacionDTO,
  PedidoInterciudadDTO,
  PlanSuscripcionDTO,
  PrestamoDetalleDTO,
  PrestamoDTO,
  ReservaLibroDTO,
  ReservaSalaDTO,
  SalaEstudioDTO,
  SancionDTO,
  SolicitudPagoDTO,
  SolicitudPedidoInterciudadDTO,
  SuscripcionDTO,
} from '../models/biblioteca.models';

@Injectable({ providedIn: 'root' })
export class OperacionesService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // Préstamos
  crearPrestamo(dto: PrestamoDTO): Observable<PrestamoDTO> {
    return this.http.post<PrestamoDTO>(`${this.api}/prestamos`, dto);
  }
  obtenerPrestamo(id: number): Observable<PrestamoDTO> {
    return this.http.get<PrestamoDTO>(`${this.api}/prestamos/${id}`);
  }
  prestamosPorUsuario(usuarioId: number): Observable<PrestamoDetalleDTO[]> {
    return this.http.get<PrestamoDetalleDTO[]>(`${this.api}/prestamos/usuario/${usuarioId}`);
  }
  prestamosPorSede(sedeId: number): Observable<PrestamoDetalleDTO[]> {
    return this.http.get<PrestamoDetalleDTO[]>(`${this.api}/prestamos/sede/${sedeId}`);
  }
  actualizarPrestamo(id: number, dto: Partial<PrestamoDTO>): Observable<PrestamoDTO> {
    return this.http.put<PrestamoDTO>(`${this.api}/prestamos/${id}`, dto);
  }

  // Reservas de Libros
  crearReservaLibro(dto: ReservaLibroDTO): Observable<ReservaLibroDTO> {
    return this.http.post<ReservaLibroDTO>(`${this.api}/reservas-libro`, dto);
  }
  reservasLibroUsuario(usuarioId: number): Observable<ReservaLibroDTO[]> {
    return this.http.get<ReservaLibroDTO[]>(`${this.api}/reservas-libro/usuario/${usuarioId}`);
  }
  reservasLibroSede(sedeId: number): Observable<ReservaLibroDTO[]> {
    return this.http.get<ReservaLibroDTO[]>(`${this.api}/reservas-libro/sede/${sedeId}`);
  }
  cancelarReservaLibro(id: number): Observable<ReservaLibroDTO> {
    return this.http.post<ReservaLibroDTO>(`${this.api}/reservas-libro/${id}/cancelar`, {});
  }

  // Devoluciones
  listarDevoluciones(): Observable<DevolucionDTO[]> {
    return this.http.get<DevolucionDTO[]>(`${this.api}/devoluciones`);
  }
  actualizarDevolucion(id: number, dto: DevolucionDTO): Observable<DevolucionDTO> {
    return this.http.put<DevolucionDTO>(`${this.api}/devoluciones/${id}`, dto);
  }
  eliminarDevolucion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/devoluciones/${id}`);
  }
  solicitarDevolucion(prestamoId: number, usuarioId: number, sedeDevolucionId: number): Observable<DevolucionDTO> {
    return this.http.post<DevolucionDTO>(`${this.api}/devoluciones/solicitar`, {
      prestamoId,
      usuarioId,
      sedeDevolucionId,
    });
  }
  confirmarDevolucionQr(codigoQr: string, sedeReceptoraId: number, bibliotecarioId: number): Observable<PrestamoDetalleDTO> {
    const params = new HttpParams()
      .set('codigoQr', codigoQr)
      .set('sedeReceptoraId', sedeReceptoraId)
      .set('bibliotecarioId', bibliotecarioId);
    return this.http.post<PrestamoDetalleDTO>(`${this.api}/devoluciones/confirmar`, {}, { params });
  }
  registrarDanioPerdida(
    prestamoId: number,
    tipoNovedad: 'DANADO' | 'PERDIDO',
    bibliotecarioId: number,
    observaciones?: string
  ): Observable<unknown> {
    let params = new HttpParams()
      .set('prestamoId', prestamoId)
      .set('tipoNovedad', tipoNovedad)
      .set('bibliotecarioId', bibliotecarioId);
    if (observaciones) params = params.set('observaciones', observaciones);
    return this.http.post(`${this.api}/devoluciones/danio-perdida`, {}, { params });
  }

  // Pedidos Interciudad
  cotizarEnvio(ciudadOrigen: string, ciudadDestino: string): Observable<{ ciudadOrigen: string; ciudadDestino: string; costoEnvio: number }> {
    const params = new HttpParams().set('ciudadOrigen', ciudadOrigen).set('ciudadDestino', ciudadDestino);
    return this.http.get<{ tarifa: number }>(`${this.api}/tarifas-envio/cotizar`, { params });
  }
  solicitarPedidoInterciudad(dto: SolicitudPedidoInterciudadDTO): Observable<PedidoInterciudadDTO> {
    return this.http.post<PedidoInterciudadDTO>(`${this.api}/pedidos-interciudad`, dto);
  }
  obtenerPedidoInterciudad(id: number): Observable<PedidoInterciudadDTO> {
    return this.http.get<PedidoInterciudadDTO>(`${this.api}/pedidos-interciudad/${id}`);
  }
  pedidosUsuario(usuarioId: number): Observable<PedidoInterciudadDTO[]> {
    return this.http.get<PedidoInterciudadDTO[]>(`${this.api}/pedidos-interciudad/usuario/${usuarioId}`);
  }
  cancelarPedidoInterciudad(id: number, usuarioId: number): Observable<PedidoInterciudadDTO> {
    const params = new HttpParams().set('usuarioId', usuarioId);
    return this.http.post<PedidoInterciudadDTO>(`${this.api}/pedidos-interciudad/${id}/cancelar`, {}, { params });
  }
  pedidosSedeOrigen(sedeId: number): Observable<PedidoInterciudadDTO[]> {
    return this.http.get<PedidoInterciudadDTO[]>(`${this.api}/pedidos-interciudad/sede-origen/${sedeId}`);
  }
  pedidosSedeDevolucion(sedeId: number): Observable<PedidoInterciudadDTO[]> {
    return this.http.get<PedidoInterciudadDTO[]>(`${this.api}/pedidos-interciudad/sede-devolucion/${sedeId}`);
  }
  despacharPedido(id: number, bibliotecarioId: number, repartidorId: number): Observable<PedidoInterciudadDTO> {
    const params = new HttpParams().set('bibliotecarioId', bibliotecarioId).set('repartidorId', repartidorId);
    return this.http.post<PedidoInterciudadDTO>(`${this.api}/pedidos-interciudad/${id}/despachar`, {}, { params });
  }
  recepcionNoRecibido(id: number, bibliotecarioId: number): Observable<PedidoInterciudadDTO> {
    const params = new HttpParams().set('bibliotecarioId', bibliotecarioId);
    return this.http.post<PedidoInterciudadDTO>(`${this.api}/pedidos-interciudad/${id}/recepcion-no-recibido`, {}, { params });
  }
  entregaEnSede(id: number, bibliotecarioId: number): Observable<PedidoInterciudadDTO> {
    const params = new HttpParams().set('bibliotecarioId', bibliotecarioId);
    return this.http.post<PedidoInterciudadDTO>(`${this.api}/pedidos-interciudad/${id}/entrega-sede`, {}, { params });
  }
  devolucionEnSede(id: number, bibliotecarioId: number, sedeId: number): Observable<PedidoInterciudadDTO> {
    const params = new HttpParams().set('bibliotecarioId', bibliotecarioId).set('sedeId', sedeId);
    return this.http.post<PedidoInterciudadDTO>(`${this.api}/pedidos-interciudad/${id}/devolucion-sede`, {}, { params });
  }
  cerrarEnOrigen(id: number, bibliotecarioId: number): Observable<PedidoInterciudadDTO> {
    const params = new HttpParams().set('bibliotecarioId', bibliotecarioId);
    return this.http.post<PedidoInterciudadDTO>(`${this.api}/pedidos-interciudad/${id}/cerrar-origen`, {}, { params });
  }

  // Repartidor
  asignacionesRepartidor(repartidorId: number): Observable<PedidoInterciudadDTO[]> {
    return this.http.get<PedidoInterciudadDTO[]>(`${this.api}/repartidores/${repartidorId}/asignaciones`);
  }
  confirmarEntregaRepartidor(repartidorId: number, pedidoId: number): Observable<PedidoInterciudadDTO> {
    return this.http.post<PedidoInterciudadDTO>(`${this.api}/repartidores/${repartidorId}/pedidos/${pedidoId}/confirmar-entrega`, {});
  }
  registrarNoRecibidoRepartidor(repartidorId: number, pedidoId: number): Observable<PedidoInterciudadDTO> {
    return this.http.post<PedidoInterciudadDTO>(`${this.api}/repartidores/${repartidorId}/pedidos/${pedidoId}/no-recibido`, {});
  }
  iniciarTrasladoRetorno(repartidorId: number, pedidoId: number): Observable<PedidoInterciudadDTO> {
    return this.http.post<PedidoInterciudadDTO>(`${this.api}/repartidores/${repartidorId}/pedidos/${pedidoId}/iniciar-traslado`, {});
  }

  // Salas de Estudio
  listarSalas(): Observable<SalaEstudioDTO[]> {
    return this.http.get<SalaEstudioDTO[]>(`${this.api}/salas-estudio`);
  }
  disponibilidadSalas(sedeId: number, inicio?: string, fin?: string): Observable<DisponibilidadSalasDTO> {
    let params = new HttpParams();
    if (inicio) params = params.set('inicio', inicio);
    if (fin) params = params.set('fin', fin);
    return this.http.get<DisponibilidadSalasDTO>(`${this.api}/salas-estudio/sede/${sedeId}/disponibilidad`, { params });
  }
  crearSala(dto: Partial<SalaEstudioDTO>): Observable<SalaEstudioDTO> {
    return this.http.post<SalaEstudioDTO>(`${this.api}/salas-estudio`, dto);
  }
  actualizarSala(id: number, dto: Partial<SalaEstudioDTO>): Observable<SalaEstudioDTO> {
    return this.http.put<SalaEstudioDTO>(`${this.api}/salas-estudio/${id}`, dto);
  }
  eliminarSala(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/salas-estudio/${id}`);
  }
  crearReservaSala(dto: ReservaSalaDTO): Observable<ReservaSalaDTO> {
    return this.http.post<ReservaSalaDTO>(`${this.api}/reservas-sala`, dto);
  }
  listarReservasSalas(): Observable<ReservaSalaDTO[]> {
    return this.http.get<ReservaSalaDTO[]>(`${this.api}/reservas-sala`);
  }
  cancelarReservaSala(id: number): Observable<ReservaSalaDTO> {
    return this.http.post<ReservaSalaDTO>(`${this.api}/reservas-sala/${id}/cancelar`, {});
  }

  // Eventos
  listarEventosProximos(): Observable<EventoDTO[]> {
    return this.http.get<EventoDTO[]>(`${this.api}/eventos/proximos`);
  }
  listarTodosEventos(): Observable<EventoDTO[]> {
    return this.http.get<EventoDTO[]>(`${this.api}/eventos`);
  }
  obtenerEvento(id: number): Observable<EventoDTO> {
    return this.http.get<EventoDTO>(`${this.api}/eventos/${id}`);
  }
  crearEvento(dto: Partial<EventoDTO>): Observable<EventoDTO> {
    return this.http.post<EventoDTO>(`${this.api}/eventos`, dto);
  }
  actualizarEvento(id: number, dto: Partial<EventoDTO>): Observable<EventoDTO> {
    return this.http.put<EventoDTO>(`${this.api}/eventos/${id}`, dto);
  }
  cancelarEvento(id: number): Observable<EventoDTO> {
    return this.http.post<EventoDTO>(`${this.api}/eventos/${id}/cancelar`, {});
  }
  eliminarEvento(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/eventos/${id}`);
  }
  inscribirEvento(dto: InscripcionEventoDTO): Observable<InscripcionEventoDTO> {
    return this.http.post<InscripcionEventoDTO>(`${this.api}/inscripciones-evento`, dto);
  }
  listarInscripciones(): Observable<InscripcionEventoDTO[]> {
    return this.http.get<InscripcionEventoDTO[]>(`${this.api}/inscripciones-evento`);
  }
  asistentesEvento(eventoId: number): Observable<AsistenteEventoDTO[]> {
    return this.http.get<AsistenteEventoDTO[]>(`${this.api}/inscripciones-evento/evento/${eventoId}`);
  }
  cancelarInscripcion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/inscripciones-evento/${id}`);
  }

  // Suscripciones y Pagos
  listarPlanes(): Observable<PlanSuscripcionDTO[]> {
    return this.http.get<PlanSuscripcionDTO[]>(`${this.api}/planes-suscripcion`);
  }
  comprarSuscripcion(usuarioId: number, planId: number, renovacionAutomatica = true): Observable<SuscripcionDTO> {
    return this.http.post<SuscripcionDTO>(`${this.api}/suscripciones/comprar`, {
      usuarioId,
      planId,
      renovacionAutomatica,
    });
  }
  cancelarSuscripcion(id: number): Observable<SuscripcionDTO> {
    return this.http.post<SuscripcionDTO>(`${this.api}/suscripciones/${id}/cancelar`, {});
  }
  multasUsuario(usuarioId: number): Observable<MultaDTO[]> {
    return this.http.get<MultaDTO[]>(`${this.api}/multas/usuario/${usuarioId}`);
  }
  sancionesUsuario(usuarioId: number): Observable<SancionDTO[]> {
    return this.http.get<SancionDTO[]>(`${this.api}/sanciones/usuario/${usuarioId}`);
  }
  procesarPago(dto: SolicitudPagoDTO): Observable<ComprobantePagoDTO> {
    return this.http.post<ComprobantePagoDTO>(`${this.api}/pagos/procesar`, dto);
  }
  pagosUsuario(usuarioId: number): Observable<ComprobantePagoDTO[]> {
    return this.http.get<ComprobantePagoDTO[]>(`${this.api}/pagos/usuario/${usuarioId}`);
  }
  notificacionesUsuario(): Observable<NotificacionDTO[]> {
    return this.http.get<NotificacionDTO[]>(`${this.api}/notificaciones`);
  }
  marcarNotificacionLeida(n: NotificacionDTO): Observable<NotificacionDTO> {
    return this.http.put<NotificacionDTO>(`${this.api}/notificaciones/${n.id}`, { ...n, leida: true });
  }
}
