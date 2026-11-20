import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuditoriaDTO,
  ChatbotRespuestaDTO,
  PagoDTO,
  PlanSuscripcionDTO,
  PreguntaFrecuenteDTO,
  TarifaEnvioDTO,
  UsuarioDTO,
} from '../models/biblioteca.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // Usuarios & Personal
  listarUsuarios(): Observable<UsuarioDTO[]> {
    return this.http.get<UsuarioDTO[]>(`${this.api}/usuarios`);
  }
  crearUsuario(dto: Partial<UsuarioDTO>): Observable<UsuarioDTO> {
    return this.http.post<UsuarioDTO>(`${this.api}/usuarios`, dto);
  }
  actualizarUsuario(id: number, dto: Partial<UsuarioDTO>): Observable<UsuarioDTO> {
    return this.http.put<UsuarioDTO>(`${this.api}/usuarios/${id}`, dto);
  }
  suspenderUsuario(id: number): Observable<UsuarioDTO> {
    return this.http.post<UsuarioDTO>(`${this.api}/usuarios/${id}/suspender`, {});
  }
  desactivarUsuario(id: number): Observable<UsuarioDTO> {
    return this.http.post<UsuarioDTO>(`${this.api}/usuarios/${id}/desactivar`, {});
  }
  reactivarUsuario(id: number): Observable<UsuarioDTO> {
    return this.http.post<UsuarioDTO>(`${this.api}/usuarios/${id}/reactivar`, {});
  }
  listarRoles(): Observable<{ id: number; nombre: string }[]> {
    return this.http.get<{ id: number; nombre: string }[]>(`${this.api}/roles`);
  }

  // Planes
  crearPlan(dto: Partial<PlanSuscripcionDTO>): Observable<PlanSuscripcionDTO> {
    return this.http.post<PlanSuscripcionDTO>(`${this.api}/planes-suscripcion`, dto);
  }
  actualizarPlan(id: number, dto: Partial<PlanSuscripcionDTO>): Observable<PlanSuscripcionDTO> {
    return this.http.put<PlanSuscripcionDTO>(`${this.api}/planes-suscripcion/${id}`, dto);
  }
  desactivarPlan(id: number): Observable<PlanSuscripcionDTO> {
    return this.http.post<PlanSuscripcionDTO>(`${this.api}/planes-suscripcion/${id}/desactivar`, {});
  }
  reactivarPlan(id: number): Observable<PlanSuscripcionDTO> {
    return this.http.post<PlanSuscripcionDTO>(`${this.api}/planes-suscripcion/${id}/reactivar`, {});
  }

  // Configuración Políticas y Plazos
  obtenerPoliticasSancion(): Observable<unknown[]> {
    return this.http.get<unknown[]>(`${this.api}/configuracion/sanciones`);
  }
  actualizarPoliticaSancion(infraccion: string, body: unknown): Observable<unknown> {
    return this.http.put(`${this.api}/configuracion/sanciones/${infraccion}`, body);
  }
  obtenerPoliticasMulta(): Observable<unknown[]> {
    return this.http.get<unknown[]>(`${this.api}/configuracion/multas`);
  }
  actualizarPoliticaMulta(concepto: string, body: unknown): Observable<unknown> {
    return this.http.put(`${this.api}/configuracion/multas/${concepto}`, body);
  }
  obtenerPlazos(): Observable<unknown[]> {
    return this.http.get<unknown[]>(`${this.api}/configuracion/plazos`);
  }
  actualizarPlazo(clave: string, body: unknown): Observable<unknown> {
    return this.http.put(`${this.api}/configuracion/plazos/${clave}`, body);
  }

  // Tarifas de envío
  listarTarifas(): Observable<TarifaEnvioDTO[]> {
    return this.http.get<TarifaEnvioDTO[]>(`${this.api}/tarifas-envio`);
  }
  guardarTarifa(dto: TarifaEnvioDTO): Observable<TarifaEnvioDTO> {
    return dto.id
      ? this.http.put<TarifaEnvioDTO>(`${this.api}/tarifas-envio/${dto.id}`, dto)
      : this.http.post<TarifaEnvioDTO>(`${this.api}/tarifas-envio`, dto);
  }

  // Reportes, Pagos y Auditoría
  tiposReporte(): Observable<string[]> {
    return this.http.get<string[]>(`${this.api}/reportes/tipos`);
  }
  generarReporteJson(tipo: string, desde?: string, hasta?: string, sedeId?: number): Observable<unknown> {
    let params = new HttpParams();
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    if (sedeId) params = params.set('sedeId', sedeId);
    return this.http.get(`${this.api}/reportes/${tipo}`, { params });
  }
  exportarReporte(tipo: string, formato: 'PDF' | 'CSV', desde?: string, hasta?: string, sedeId?: number): Observable<Blob> {
    let params = new HttpParams().set('formato', formato);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    if (sedeId) params = params.set('sedeId', sedeId);
    return this.http.get(`${this.api}/reportes/${tipo}/exportar`, { params, responseType: 'blob' });
  }
  filtrarPagos(concepto?: string, estado?: string, desde?: string, hasta?: string): Observable<PagoDTO[]> {
    let params = new HttpParams();
    if (concepto) params = params.set('concepto', concepto);
    if (estado) params = params.set('estado', estado);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    return this.http.get<PagoDTO[]>(`${this.api}/pagos/filtrar`, { params });
  }
  listarAuditoria(modulo?: string, accion?: string): Observable<AuditoriaDTO[]> {
    let params = new HttpParams();
    if (modulo) params = params.set('modulo', modulo);
    if (accion) params = params.set('accion', accion);
    return this.http.get<AuditoriaDTO[]>(`${this.api}/auditoria/filtros`, { params });
  }

  // Chatbot & FAQ
  enviarMensajeChatbot(mensaje: string): Observable<ChatbotRespuestaDTO> {
    return this.http.post<ChatbotRespuestaDTO>(`${this.api}/chatbot/mensaje`, { mensaje });
  }
  listarFaqs(): Observable<PreguntaFrecuenteDTO[]> {
    return this.http.get<PreguntaFrecuenteDTO[]>(`${this.api}/preguntas-frecuentes`);
  }
  crearFaq(dto: PreguntaFrecuenteDTO): Observable<PreguntaFrecuenteDTO> {
    return this.http.post<PreguntaFrecuenteDTO>(`${this.api}/preguntas-frecuentes`, dto);
  }
  actualizarFaq(id: number, dto: PreguntaFrecuenteDTO): Observable<PreguntaFrecuenteDTO> {
    return this.http.put<PreguntaFrecuenteDTO>(`${this.api}/preguntas-frecuentes/${id}`, dto);
  }
  eliminarFaq(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/preguntas-frecuentes/${id}`);
  }
}
