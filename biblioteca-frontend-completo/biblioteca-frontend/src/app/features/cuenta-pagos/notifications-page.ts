import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { NotificacionDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-notifications-page',
  imports: [DatePipe, MainTemplate, Card, Badge, Button],
  template: `
    <app-main-template titulo="Centro de Notificaciones">
      <div class="grid-cards">
        @for (n of notificaciones(); track n.id) {
          <app-card [destacada]="!n.leida">
            <div cardEncabezado class="head">
              <strong>{{ n.titulo }}</strong>
              <app-badge [texto]="n.leida ? 'Leída' : 'Nueva'" [variante]="n.leida ? 'neutro' : 'info'" />
            </div>
            <p>{{ n.mensaje }}</p>
            <small>{{ n.fechaCreacion | date:'d MMM y, h:mm a' }}</small>
            @if (!n.leida) {
              <div cardPie>
                <app-button texto="Marcar como leída" variante="contorno" tamano="pequeno" (presionado)="leer(n)" />
              </div>
            }
          </app-card>
        }
      </div>
    </app-main-template>
  `,
  styles: [`.head { display: flex; justify-content: space-between; }`]
})
export class NotificationsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);

  protected readonly notificaciones = signal<NotificacionDTO[]>([]);

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    const uid = this.auth.usuarioId()!;
    this.ops.notificacionesUsuario().subscribe((all) =>
      this.notificaciones.set(all.filter((n) => n.usuarioId === uid))
    );
  }

  protected leer(n: NotificacionDTO): void {
    this.ops.marcarNotificacionLeida(n).subscribe(() => this.cargar());
  }
}
