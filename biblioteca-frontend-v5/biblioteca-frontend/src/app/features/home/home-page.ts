import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { EventoDTO, LibroDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { TruncarPipe } from '../../shared/pipes/truncar.pipe';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-home-page',
  imports: [FormsModule, RouterLink, DatePipe, MainTemplate, Card, Badge, Button, TruncarPipe],
  template: `
    <app-main-template>
      <!-- Hero Principal -->
      <section class="hero">
        <div class="hero__content">
          <span class="hero__tag">Red de Bibliotecas Empresariales</span>
          @if (auth.autenticado()) {
            <h1>¡Hola, {{ auth.nombreMostrar() }}! ¿Qué quieres leer hoy?</h1>
          } @else {
            <h1>Descubre miles de libros, salas de estudio y eventos culturales</h1>
          }
          <p class="hero__sub">
            Consulta disponibilidad en tiempo real por sede, solicita préstamos locales o pide libros de otras ciudades directamente a tu domicilio.
          </p>

          <form class="hero__search" (ngSubmit)="buscarRapido()">
            <input
              class="campo"
              placeholder="Busca por título de libro..."
              [(ngModel)]="terminoBusqueda"
              name="q"
            />
            <app-button tipo="submit" texto="Buscar en Catálogo" />
          </form>

          <div class="hero__actions">
            <a routerLink="/catalogo" class="hero__link">Explorar Catálogo</a>
            <a routerLink="/sedes" class="hero__link">Sedes y Salas</a>
            <a routerLink="/eventos" class="hero__link">Eventos Próximos</a>
            @if (!auth.esSuscriptor()) {
              <a routerLink="/planes" class="hero__link hero__link--dest">Conocer Suscripción</a>
            }
          </div>
        </div>
      </section>

      <!-- Accesos rápidos si está autenticado -->
      @if (auth.autenticado() && auth.rol() === 'CLIENTE') {
        <section class="seccion">
          <h2>Tu Espacio Personal</h2>
          <div class="quick-grid">
            <a routerLink="/mi-cuenta/prestamos" class="quick-card">
              <strong>Mis Préstamos y Reservas</strong>
              <span>Consulta fechas límite y genera tu QR de devolución.</span>
            </a>
            <a routerLink="/mi-cuenta/salas" class="quick-card">
              <strong>Salas de Estudio</strong>
              <span>Administra tus espacios reservados.</span>
            </a>
            <a routerLink="/mi-cuenta/multas" class="quick-card">
              <strong>Multas y Pagos</strong>
              <span>Revisa tu estado de cuenta y comprobantes.</span>
            </a>
            @if (auth.esSuscriptor()) {
              <a routerLink="/mi-cuenta/pedidos" class="quick-card quick-card--sub">
                <strong>Pedidos Interciudad</strong>
                <span>Sigue tus envíos a domicilio entre ciudades.</span>
              </a>
            }
          </div>
        </section>
      }

      <!-- Libros Destacados -->
      <section class="seccion">
        <div class="seccion__head">
          <h2>Libros Disponibles en Catálogo</h2>
          <a routerLink="/catalogo">Ver catálogo completo →</a>
        </div>
        <div class="grid-cards">
          @for (libro of librosDestacados(); track libro.id) {
            <app-card>
              <div cardEncabezado class="row-between">
                <app-badge [texto]="libro.idioma" />
                <small>{{ libro.anioPublicacion }}</small>
              </div>
              <h3>{{ libro.titulo }}</h3>
              <p class="texto-apoyo">{{ libro.editorial || 'Editorial General' }}</p>
              <p>{{ libro.sinopsis | truncar:95 }}</p>
              <div cardPie>
                <a [routerLink]="['/catalogo', libro.id]" class="card-cta">Ver disponibilidad →</a>
              </div>
            </app-card>
          } @empty {
            <p class="texto-apoyo">Aún no hay libros registrados en el catálogo.</p>
          }
        </div>
      </section>

      <!-- Próximos Eventos -->
      <section class="seccion">
        <div class="seccion__head">
          <h2>Próximos Eventos</h2>
          <a routerLink="/eventos">Ver todos los eventos →</a>
        </div>
        <div class="grid-cards">
          @for (ev of eventosProximos(); track ev.id) {
            <app-card [destacada]="ev.exclusivo">
              <div cardEncabezado class="row-between">
                <app-badge [texto]="ev.exclusivo ? 'Exclusivo Suscriptores' : 'Abierto'" [variante]="ev.exclusivo ? 'advertencia' : 'info'" />
                <small>{{ ev.cuposDisponibles }} cupos</small>
              </div>
              <h3>{{ ev.titulo }}</h3>
              <p class="texto-apoyo">{{ ev.fechaInicio | date:'d MMM y, h:mm a' }}</p>
              <p>{{ ev.descripcion | truncar:90 }}</p>
              <div cardPie>
                <a [routerLink]="['/eventos', ev.id]" class="card-cta">Ver detalle →</a>
              </div>
            </app-card>
          } @empty {
            <p class="texto-apoyo">No hay eventos próximos programados.</p>
          }
        </div>
      </section>

      <!-- Sedes Activas -->
      <section class="seccion">
        <div class="seccion__head">
          <h2>Nuestras Sedes en Operación</h2>
          <a routerLink="/sedes">Ver todas las sedes →</a>
        </div>
        <div class="grid-cards">
          @for (s of sedes(); track s.id) {
            <app-card>
              <div cardEncabezado class="row-between">
                <strong>{{ s.ciudad }}</strong>
                <app-badge texto="Activa" estado="ACTIVA" />
              </div>
              <h3>{{ s.nombre }}</h3>
              <p class="texto-apoyo">{{ s.direccion }}</p>
            </app-card>
          }
        </div>
      </section>
    </app-main-template>
  `,
  styles: [`
    .hero {
      background: linear-gradient(135deg, #1e3a8a 0%, #1d4ed8 100%);
      color: #fff;
      border-radius: var(--radio-lg);
      padding: 2.75rem 2rem;
      margin-bottom: 2rem;
      box-shadow: var(--sombra-md);
      h1 { color: #fff; font-size: 2.1rem; margin: 0.5rem 0 0.75rem; max-width: 720px; }
    }
    .hero__tag { display: inline-block; background: rgba(255,255,255,0.18); padding: 0.25rem 0.75rem; border-radius: 999px; font-size: 0.82rem; font-weight: 700; }
    .hero__sub { color: #e2e8f0; max-width: 660px; margin-bottom: 1.5rem; font-size: 1.02rem; }
    .hero__search { display: flex; gap: 0.6rem; max-width: 560px; margin-bottom: 1.25rem; flex-wrap: wrap; input { flex: 1; min-width: 220px; } }
    .hero__actions { display: flex; gap: 1rem; flex-wrap: wrap; }
    .hero__link { color: #fff; text-decoration: none; font-weight: 700; font-size: 0.92rem; padding: 0.35rem 0.75rem; border-radius: var(--radio); background: rgba(255,255,255,0.12); &:hover { background: rgba(255,255,255,0.22); } }
    .hero__link--dest { background: #f59e0b; color: #0f172a; }

    .seccion { margin-bottom: 2.25rem; }
    .seccion__head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 1rem; a { color: var(--color-primario); font-weight: 700; text-decoration: none; } }
    .quick-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; }
    .quick-card {
      background: var(--color-superficie); border: 1px solid var(--color-borde); border-radius: var(--radio-lg);
      padding: 1.15rem; text-decoration: none; color: var(--color-texto); display: flex; flex-direction: column; gap: 0.3rem;
      box-shadow: var(--sombra-sm); transition: transform 0.15s, border-color 0.15s;
      &:hover { transform: translateY(-2px); border-color: var(--color-primario); }
      strong { color: var(--color-primario); font-size: 1.02rem; }
      span { font-size: 0.86rem; color: var(--color-texto-apoyo); }
    }
    .quick-card--sub { border-color: var(--color-secundario); strong { color: var(--color-secundario); } }
    .row-between { display: flex; justify-content: space-between; align-items: center; }
    .texto-apoyo { color: var(--color-texto-apoyo); font-size: 0.88rem; margin: 0.2rem 0 0.5rem; }
    .card-cta { color: var(--color-primario); font-weight: 700; text-decoration: none; }
  `]
})
export class HomePage implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly catalogo = inject(CatalogoService);
  private readonly ops = inject(OperacionesService);
  private readonly router = inject(Router);

  protected readonly librosDestacados = signal<LibroDTO[]>([]);
  protected readonly eventosProximos = signal<EventoDTO[]>([]);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected terminoBusqueda = '';

  ngOnInit(): void {
    this.catalogo.buscarLibros().subscribe((l) => this.librosDestacados.set(l.slice(0, 6)));
    this.ops.listarEventosProximos().subscribe((e) => this.eventosProximos.set(e.slice(0, 3)));
    this.catalogo.listarSedesActivas().subscribe((s) => this.sedes.set(s.slice(0, 3)));
  }

  protected buscarRapido(): void {
    const q = this.terminoBusqueda.trim();
    this.router.navigate(['/catalogo'], q ? { queryParams: { titulo: q } } : {});
  }
}
