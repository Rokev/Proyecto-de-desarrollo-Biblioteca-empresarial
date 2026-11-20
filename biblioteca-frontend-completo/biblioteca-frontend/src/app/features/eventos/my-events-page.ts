import { Component, inject, OnInit, signal } from '@angular/core';
import { InscripcionEventoDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-my-events-page',
  imports: [AccountTemplate, Card, Badge, Button],
  template: `
    <app-account-template titulo="Mis Inscripciones a Eventos">
      <div class="grid-cards">
        @for (ins of inscripciones(); track ins.id) {
          <app-card>
            <div cardEncabezado class="head">
              <strong>Evento #{{ ins.eventoId }}</strong>
              <app-badge [texto]="ins.estado || 'CONFIRMADA'" [estado]="ins.estado || 'CONFIRMADA'" />
            </div>
            <p><strong>Cupos reservados:</strong> {{ ins.cantidadCupos }}</p>
            <div cardPie>
              <app-button texto="Cancelar Inscripción" variante="peligro" tamano="pequeno" (presionado)="cancelar(ins.id!)" />
            </div>
          </app-card>
        }
      </div>
    </app-account-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; }`]
})
export class MyEventsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly inscripciones = signal<InscripcionEventoDTO[]>([]);

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    const uid = this.auth.usuarioId()!;
    this.ops.listarInscripciones().subscribe((all) =>
      this.inscripciones.set(all.filter((i) => i.usuarioId === uid))
    );
  }

  protected cancelar(id: number): void {
    this.ops.cancelarInscripcion(id).subscribe(() => {
      this.ui.mostrar('Inscripción cancelada.', 'info');
      this.cargar();
    });
  }
}
