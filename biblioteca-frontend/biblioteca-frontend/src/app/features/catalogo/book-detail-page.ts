import { Component, computed, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AutorDTO, EjemplarDTO, GeneroDTO, LibroDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-book-detail-page',
  imports: [RouterLink, MainTemplate, Card, Badge, Button, ConfirmDialog],
  template: `
    <app-main-template [titulo]="libro()?.titulo || 'Detalle del libro'">
      <p><a routerLink="/catalogo">← Volver al catálogo</a></p>
      @if (libro(); as l) {
        <app-card>
          <div cardEncabezado class="head">
            <span>ISBN: <strong>{{ l.isbn }}</strong></span>
            <app-badge [texto]="l.idioma" />
          </div>
          <p><strong>Autor:</strong> {{ autoresTexto() }} · <strong>Género:</strong> {{ generoTexto() }}</p>
          <p><strong>Editorial:</strong> {{ l.editorial || 'N/A' }} · <strong>Año:</strong> {{ l.anioPublicacion }} · <strong>Páginas:</strong> {{ l.numeroPaginas || 'N/A' }}</p>
          <p>{{ l.sinopsis || 'Sin sinopsis registrada.' }}</p>

          <h3>Disponibilidad por Sede</h3>
          <div class="tabla-responsive">
            <table class="tabla">
              <thead>
                <tr><th>Sede</th><th>Ciudad</th><th>Disponibles</th><th>Acciones</th></tr>
              </thead>
              <tbody>
                @for (fila of disponibilidadPorSede(); track fila.sede.id) {
                  <tr>
                    <td>{{ fila.sede.nombre }}</td>
                    <td>{{ fila.sede.ciudad }}</td>
                    <td>
                      <app-badge
                        [texto]="fila.disponibles > 0 ? fila.disponibles + ' disponibles' : 'Sin stock'"
                        [estado]="fila.disponibles > 0 ? 'DISPONIBLE' : 'VENCIDO'"
                      />
                    </td>
                    <td class="acciones-fila">
                      @if (fila.disponibles > 0) {
                        <app-button texto="Solicitar Préstamo" tamano="pequeno" (presionado)="irAPrestamo(l.id, fila.sede.id)" />
                        <!-- Prueba 52: mostrar siempre el botón interciudad; si no tiene suscripción lleva al aviso -->
                        <app-button texto="Pedido Interciudad" variante="secundario" tamano="pequeno" (presionado)="irAInterciudad(l.id)" />
                      } @else {
                        <app-button texto="Reservar sin stock" variante="contorno" tamano="pequeno" (presionado)="pedirConfirmReserva(l.id, fila.sede)" />
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </app-card>
      }

      <app-confirm-dialog
        [(abierto)]="confirmarReservaAbierto"
        titulo="Confirmar Reserva de Libro"
        [mensaje]="'¿Deseas reservar este libro sin stock en la sede ' + (sedeReserva()?.nombre || '') + '? Te notificaremos cuando esté disponible.'"
        textoConfirmar="Confirmar Reserva"
        (confirmar)="ejecutarReserva()"
      />
    </app-main-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; } .acciones-fila { display: flex; gap: 0.5rem; flex-wrap: wrap; }`]
})
export class BookDetailPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly ops = inject(OperacionesService);
  protected readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);
  private readonly router = inject(Router);

  readonly libroId = input.required({ transform: numberAttribute });
  protected readonly libro = signal<LibroDTO | null>(null);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly ejemplares = signal<EjemplarDTO[]>([]);
  protected readonly autores = signal<AutorDTO[]>([]);
  protected readonly generos = signal<GeneroDTO[]>([]);

  protected readonly confirmarReservaAbierto = signal(false);
  protected readonly sedeReserva = signal<SedeDTO | null>(null);

  protected readonly autoresTexto = computed(() => {
    const ids = this.libro()?.autoresIds ?? [];
    const nombres = this.autores()
      .filter((a) => a.id !== undefined && ids.includes(a.id))
      .map((a) => a.nombre);
    return nombres.length ? nombres.join(', ') : 'N/A';
  });

  protected readonly generoTexto = computed(() => {
    const id = this.libro()?.generoId;
    return this.generos().find((g) => g.id === id)?.nombre ?? 'N/A';
  });

  protected readonly disponibilidadPorSede = computed(() => {
    const id = this.libroId();
    const ejs = this.ejemplares().filter((e) => e.libroId === id && e.estado === 'DISPONIBLE');
    return this.sedes().map((sede) => ({
      sede,
      disponibles: ejs.filter((e) => e.sedeId === sede.id).length,
    }));
  });

  ngOnInit(): void {
    this.catalogo.obtenerLibro(this.libroId()).subscribe((l) => this.libro.set(l));
    this.catalogo.listarSedesActivas().subscribe((s) => this.sedes.set(s));
    this.catalogo.listarEjemplares().subscribe((e) => this.ejemplares.set(e));
    this.catalogo.listarAutores().subscribe((a) => this.autores.set(a));
    this.catalogo.listarGeneros().subscribe((g) => this.generos.set(g));
  }

  protected irAPrestamo(libroId: number, sedeId: number): void {
    if (!this.auth.autenticado()) { this.router.navigate(['/login']); return; }
    this.router.navigate(['/catalogo', libroId, 'solicitar'], { queryParams: { sedeId } });
  }

  protected irAInterciudad(libroId: number): void {
    if (!this.auth.autenticado()) { this.router.navigate(['/login']); return; }
    if (!this.auth.esSuscriptor()) {
      this.router.navigate(['/planes'], { queryParams: { motivo: 'suscripcion' } });
      return;
    }
    this.router.navigate(['/pedidos/nuevo'], { queryParams: { libroId } });
  }

  protected pedirConfirmReserva(libroId: number, sede: SedeDTO): void {
    if (!this.auth.autenticado()) { this.router.navigate(['/login']); return; }
    this.sedeReserva.set(sede);
    this.confirmarReservaAbierto.set(true);
  }

  protected ejecutarReserva(): void {
    const usuarioId = this.auth.usuarioId();
    const sede = this.sedeReserva();
    if (!usuarioId || !sede) return;
    this.ops.crearReservaLibro({ usuarioId, libroId: this.libroId(), sedeId: sede.id }).subscribe({
      next: () => {
        this.ui.mostrar('Reserva registrada. Te notificaremos cuando haya stock disponible.', 'exito');
        this.router.navigate(['/mi-cuenta/reservas']);
      },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}