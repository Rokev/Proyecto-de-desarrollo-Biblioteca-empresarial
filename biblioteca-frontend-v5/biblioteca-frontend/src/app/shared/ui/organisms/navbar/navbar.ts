import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { Button } from '../../atoms/button/button';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, Button],
  template: `
    <header class="navbar">
      <div class="navbar__contenedor">
        <a routerLink="/" class="navbar__logo">Biblioteca Red</a>
        <nav class="navbar__nav" aria-label="Principal">
          @for (item of enlaces(); track item.ruta) {
            <a [routerLink]="item.ruta" routerLinkActive="navbar__link--activo" class="navbar__link">{{ item.texto }}</a>
          }
        </nav>
        <div class="navbar__acciones">
          @if (auth.autenticado()) {
            <a routerLink="/notificaciones" class="navbar__link" aria-label="Notificaciones">🔔 Avisos</a>
            <a routerLink="/mi-cuenta/perfil" class="navbar__usuario">{{ auth.nombreMostrar() }}</a>
            <app-button texto="Salir" variante="contorno" tamano="pequeno" (presionado)="auth.cerrarSesion()" />
          } @else {
            <a routerLink="/login" class="navbar__link">Ingresar</a>
            <a routerLink="/registro" class="navbar__btn">Registrarse</a>
          }
        </div>
      </div>
    </header>
  `,
  styles: [`
    .navbar { background: var(--color-superficie); border-bottom: 1px solid var(--color-borde); position: sticky; top: 0; z-index: 50; }
    .navbar__contenedor { max-width: var(--max-width); margin: 0 auto; padding: 0.75rem 1.25rem; display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
    .navbar__logo { text-decoration: none; font-family: var(--font-heading); font-size: 1.35rem; font-weight: 700; color: var(--color-primario); }
    .navbar__nav, .navbar__acciones { display: flex; align-items: center; gap: 0.9rem; flex-wrap: wrap; }
    .navbar__link { text-decoration: none; color: var(--color-texto); font-weight: 600; padding: 0.35rem 0.6rem; border-radius: var(--radio); &:hover, &--activo { color: var(--color-primario); background: var(--color-primario-suave); } }
    .navbar__usuario { font-weight: 700; color: var(--color-texto); text-decoration: none; }
    .navbar__btn { background: var(--color-primario); color: #fff; padding: 0.45rem 0.9rem; border-radius: var(--radio); text-decoration: none; font-weight: 700; font-size: 0.9rem; }
  `]
})
export class Navbar {
  protected readonly auth = inject(AuthService);

  protected readonly enlaces = computed(() => {
    const rol = this.auth.rol();
    if (rol === 'REPARTIDOR') return [{ ruta: '/reparto', texto: 'Mis Entregas y Traslados' }];
    if (rol === 'BIBLIOTECARIO') return [
      { ruta: '/panel', texto: 'Panel Bibliotecario' },
      { ruta: '/catalogo', texto: 'Catálogo' },
      { ruta: '/eventos', texto: 'Eventos' },
    ];
    if (rol === 'ADMIN') return [
      { ruta: '/admin/usuarios', texto: 'Administración' },
      { ruta: '/catalogo', texto: 'Catálogo' },
      { ruta: '/sedes', texto: 'Sedes' },
    ];
    const base = [
      { ruta: '/inicio', texto: 'Inicio' },
      { ruta: '/catalogo', texto: 'Catálogo' },
      { ruta: '/sedes', texto: 'Sedes y Salas' },
      { ruta: '/eventos', texto: 'Eventos' },
      { ruta: '/planes', texto: 'Suscripción' },
    ];
    if (this.auth.autenticado()) base.push({ ruta: '/mi-cuenta/prestamos', texto: 'Mi Cuenta' });
    return base;
  });
}
