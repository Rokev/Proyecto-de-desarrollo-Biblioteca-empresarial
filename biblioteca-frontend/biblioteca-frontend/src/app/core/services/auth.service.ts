import { HttpClient, HttpContext } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, switchMap, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SIN_INDICADOR_CARGA, SIN_NOTIFICACION_ERROR } from '../interceptors/contexto';
import {
  AuthResponseDTO,
  LoginRequestDTO,
  RegistroDTO,
  RolNombre,
  SuscripcionDTO,
  UsuarioDTO,
} from '../models/biblioteca.models';

const KEY_AUTH = 'biblio.auth';
const KEY_USER = 'biblio.user';
const KEY_SUB = 'biblio.sub';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiAuth = `${environment.apiUrl}/auth`;

  private readonly authData = signal<AuthResponseDTO | null>(this.leer<AuthResponseDTO>(KEY_AUTH));
  private readonly perfilData = signal<UsuarioDTO | null>(this.leer<UsuarioDTO>(KEY_USER));
  private readonly suscripcionData = signal<SuscripcionDTO | null>(this.leer<SuscripcionDTO>(KEY_SUB));

  readonly token = computed(() => this.authData()?.token ?? null);
  readonly autenticado = computed(() => this.authData() !== null);
  readonly usuarioId = computed(() => this.authData()?.usuarioId ?? null);
  readonly nombreMostrar = computed(() => this.perfilData()?.nombre ?? this.authData()?.nombre ?? '');
  readonly rol = computed<RolNombre | null>(() => this.authData()?.rol ?? null);
  readonly perfil = this.perfilData.asReadonly();
  readonly suscripcionVigente = this.suscripcionData.asReadonly();

  // Pruebas 49 y 50: una suscripción ACTIVA o CANCELADA sigue vigente hasta su fechaFin
  readonly esSuscriptor = computed(() => {
    const sub = this.suscripcionData();
    if (!sub) return false;
    if (sub.estado !== 'ACTIVA' && sub.estado !== 'CANCELADA') return false;
    if (!sub.fechaFin) return sub.estado === 'ACTIVA';
    const hoy = new Date().toISOString().slice(0, 10);
    return sub.fechaFin >= hoy;
  });

  login(dto: LoginRequestDTO): Observable<UsuarioDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiAuth}/login`, dto).pipe(
      tap((res) => {
        this.authData.set(res);
        this.guardar(KEY_AUTH, res);
      }),
      switchMap((res) => this.cargarPerfilYSuscripcion(res.usuarioId, res.rol))
    );
  }

  registrar(dto: RegistroDTO): Observable<UsuarioDTO> {
    return this.http.post<UsuarioDTO>(`${this.apiAuth}/registro`, dto);
  }

  solicitarRecuperacion(correo: string): Observable<{ mensaje: string; token?: string }> {
    return this.http.post<{ mensaje: string; token?: string }>(`${this.apiAuth}/recuperar`, { correo });
  }

  restablecerContrasena(token: string, nuevaContrasena: string): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${this.apiAuth}/restablecer`, { token, nuevaContrasena });
  }

  cargarPerfilYSuscripcion(usuarioId: number, rol: RolNombre): Observable<UsuarioDTO> {
    const context = new HttpContext().set(SIN_INDICADOR_CARGA, true).set(SIN_NOTIFICACION_ERROR, true);
    return this.http.get<UsuarioDTO>(`${this.apiAuth}/me`, { context }).pipe(
      tap((u) => {
        this.perfilData.set(u);
        this.guardar(KEY_USER, u);
        if (rol === 'CLIENTE') {
          this.refrescarSuscripcion(usuarioId);
        }
      })
    );
  }

  refrescarSuscripcion(usuarioId: number): void {
    const context = new HttpContext().set(SIN_INDICADOR_CARGA, true).set(SIN_NOTIFICACION_ERROR, true);
    this.http
      .get<SuscripcionDTO>(`${environment.apiUrl}/suscripciones/usuario/${usuarioId}/vigente`, { context })
      .subscribe({
        next: (sub) => {
          this.suscripcionData.set(sub);
          this.guardar(KEY_SUB, sub);
        },
        error: () => {
          this.suscripcionData.set(null);
          localStorage.removeItem(KEY_SUB);
        },
      });
  }

  actualizarPerfil(datos: Partial<UsuarioDTO>): Observable<UsuarioDTO> {
    return this.http.put<UsuarioDTO>(`${this.apiAuth}/me`, datos).pipe(
      tap((u) => {
        this.perfilData.set(u);
        this.guardar(KEY_USER, u);
      })
    );
  }

  actualizarUsuarioCompleto(id: number, datos: UsuarioDTO): Observable<UsuarioDTO> {
    return this.http.put<UsuarioDTO>(`${environment.apiUrl}/usuarios/${id}`, datos).pipe(
      tap((u) => {
        this.perfilData.set(u);
        this.guardar(KEY_USER, u);
      })
    );
  }

  cambiarContrasena(contrasenaActual: string, nuevaContrasena: string): Observable<{ mensaje: string }> {
    return this.http.put<{ mensaje: string }>(`${this.apiAuth}/me/contrasena`, {
      contrasenaActual,
      nuevaContrasena,
    });
  }

  cerrarSesion(redirigir = true): void {
    if (this.token()) {
      this.http.post(`${this.apiAuth}/logout`, {}).subscribe({ error: () => {} });
    }
    this.authData.set(null);
    this.perfilData.set(null);
    this.suscripcionData.set(null);
    localStorage.removeItem(KEY_AUTH);
    localStorage.removeItem(KEY_USER);
    localStorage.removeItem(KEY_SUB);
    if (redirigir) this.router.navigate(['/login']);
  }

  redirigirSegunRol(): void {
    switch (this.rol()) {
      case 'ADMIN': this.router.navigate(['/admin/usuarios']); break;
      case 'BIBLIOTECARIO': this.router.navigate(['/panel']); break;
      case 'REPARTIDOR': this.router.navigate(['/reparto']); break;
      default: this.router.navigate(['/inicio']);
    }
  }

  private guardar(k: string, v: unknown): void {
    try { localStorage.setItem(k, JSON.stringify(v)); } catch {}
  }

  private leer<T>(k: string): T | null {
    try {
      const raw = localStorage.getItem(k);
      return raw ? (JSON.parse(raw) as T) : null;
    } catch { return null; }
  }
}
