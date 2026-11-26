import { DatePipe } from '@angular/common';
import { Component, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { EventoDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-event-detail-page',
  imports: [FormsModule, RouterLink, DatePipe, MainTemplate, Card, Badge, Button, Alert],
  template: `
    <app-main-template [titulo]="evento()?.titulo || 'Detalle del Evento'">
      <p><a routerLink="/eventos">← Volver a eventos</a></p>
      @if (evento(); as ev) {
        <app-card>
          <div cardEncabezado class="head">
            <app-badge [texto]="ev.exclusivo ? 'Exclusivo Suscriptores' : 'Abierto'" [variante]="ev.exclusivo ? 'advertencia' : 'exito'" />
            <span>Cupos: <strong>{{ ev.cuposDisponibles }} disponibles</strong> / {{ ev.cupoMaximo }} totales</span>
          </div>
          <!-- Prueba 38: mostrar sede y cupo máximo -->
          <p><strong>Sede:</strong> {{ sedeNombre() }}</p>
          <p><strong>Inicio:</strong> {{ ev.fechaInicio | date:'d MMM y, h:mm a' }} · <strong>Fin:</strong> {{ ev.fechaFin | date:'h:mm a' }}</p>
          <p>{{ ev.descripcion }}</p>

          @if (ev.exclusivo && !auth.esSuscriptor()) {
            <app-alert
              titulo="Evento exclusivo para suscriptores"
              mensaje="Adquiere un plan de suscripción vigente para reservar cupos en este evento."
              tipo="advertencia"
            />
          } @else {
            <div class="inscripcion">
              <span>Límite: <strong>1 cupo por usuario</strong></span>
              <app-button texto="Reservar Cupo" [deshabilitado]="ev.cuposDisponibles <= 0" (presionado)="reservar(ev.id)" />
            </div>
          }
        </app-card>
      }
    </app-main-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; } .inscripcion { display: flex; align-items: center; gap: 1rem; margin-top: 1rem; }`]
})
export class EventDetailPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly catalogo = inject(CatalogoService);
  protected readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);
  private readonly router = inject(Router);

  readonly eventoId = input.required({ transform: numberAttribute });
  protected readonly evento = signal<EventoDTO | null>(null);
  protected readonly sedeNombre = signal<string>('');
  protected cupos = 1;

  ngOnInit(): void {
    this.ops.obtenerEvento(this.eventoId()).subscribe((e) => {
      this.evento.set(e);
      this.catalogo.listarSedesActivas().subscribe((sedes) => {
        const s = sedes.find((x) => x.id === e.sedeId);
        this.sedeNombre.set(s ? `${s.nombre} (${s.ciudad})` : `Sede #${e.sedeId}`);
      });
    });
  }

  protected reservar(eventoId: number): void {
    if (!this.auth.autenticado()) { this.router.navigate(['/login']); return; }
    this.ops.inscribirEvento({
      usuarioId: this.auth.usuarioId()!,
      eventoId,
      cantidadCupos: 1,
    }).subscribe({
      next: () => {
        this.ui.mostrar('Inscripción al evento confirmada.', 'exito');
        this.router.navigate(['/mi-cuenta/eventos']);
      },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}