import { Component, inject, OnInit, signal } from '@angular/core';
import { EventoDTO, InscripcionEventoDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-my-events-page',
  imports: [AccountTemplate, Card, Badge, Button, ConfirmDialog],
  template: `
    <app-account-template titulo="Mis Inscripciones a Eventos">
      <div class="grid-cards">
        @for (ins of inscripciones(); track ins.id) {
          <app-card>
            <div cardEncabezado class="head">
              <!-- Prueba 41: mostrar título del evento y lista de asistentes -->
              <strong>{{ tituloEvento(ins.eventoId) }}</strong>
              <app-badge [texto]="ins.estado || 'CONFIRMADA'" [estado]="ins.estado || 'CONFIRMADA'" />
            </div>
            <p><strong>Cupos reservados:</strong> {{ ins.cantidadCupos }}</p>
            <p><strong>Asistentes registrados en evento:</strong> {{ conteoAsistentes().get(ins.eventoId) ?? 0 }}</p>
            <div cardPie>
              <app-button texto="Cancelar Inscripción" variante="peligro" tamano="pequeno" (presionado)="pedirCancelar(ins.id!)" />
            </div>
          </app-card>
        }
      </div>

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        titulo="Cancelar Inscripción a Evento"
        mensaje="¿Confirmas que deseas cancelar tu inscripción y liberar los cupos?"
        textoConfirmar="Sí, cancelar"
        [peligro]="true"
        (confirmar)="ejecutarCancelar()"
      />
    </app-account-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; align-items: center; }`]
})
export class MyEventsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly inscripciones = signal<InscripcionEventoDTO[]>([]);
  protected readonly eventos = signal<EventoDTO[]>([]);
  protected readonly conteoAsistentes = signal<Map<number, number>>(new Map());
  protected readonly confirmOpen = signal(false);
  protected readonly idSel = signal<number | null>(null);

  ngOnInit(): void {
    this.ops.listarTodosEventos().subscribe((evs) => this.eventos.set(evs));
    this.cargar();
  }

  private cargar(): void {
    const uid = this.auth.usuarioId()!;
    this.ops.listarInscripciones().subscribe((all) => {
      const mias = all.filter((i) => i.usuarioId === uid);
      this.inscripciones.set(mias);
      mias.forEach((ins) => {
        this.ops.asistentesEvento(ins.eventoId).subscribe((lista) => {
          this.conteoAsistentes.update((m) => {
            const copy = new Map(m);
            copy.set(ins.eventoId, lista.length);
            return copy;
          });
        });
      });
    });
  }

  protected tituloEvento(eventoId: number): string {
    return this.eventos().find((e) => e.id === eventoId)?.titulo ?? `Evento #${eventoId}`;
  }

  protected pedirCancelar(id: number): void {
    this.idSel.set(id);
    this.confirmOpen.set(true);
  }

  protected ejecutarCancelar(): void {
    const id = this.idSel();
    if (!id) return;
    this.ops.cancelarInscripcion(id).subscribe(() => {
      this.ui.mostrar('Inscripción cancelada y cupos liberados.', 'info');
      this.cargar();
    });
  }
}
