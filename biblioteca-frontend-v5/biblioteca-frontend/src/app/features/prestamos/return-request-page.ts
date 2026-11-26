import { Component, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { DevolucionDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-return-request-page',
  imports: [FormsModule, RouterLink, MainTemplate, Card, FormField, Button, Alert],
  template: `
    <app-main-template titulo="Devolver Libro" subtitulo="Puedes devolver préstamos normales en cualquier sede activa de la red.">
      <p><a routerLink="/mi-cuenta/prestamos">← Volver a mis préstamos</a></p>
      @if (error()) { <app-alert titulo="Error" [mensaje]="error()!" tipo="error" /> }
      <app-card>
        <form (ngSubmit)="generarNuevoQr()">
          <app-form-field etiqueta="Selecciona la sede donde entregarás el libro" para="sedeDev" [obligatorio]="true">
            <select id="sedeDev" class="campo" [(ngModel)]="sedeId" name="sedeDev">
              @for (s of sedes(); track s.id) {
                <option [ngValue]="s.id">{{ s.nombre }} ({{ s.ciudad }})</option>
              }
            </select>
          </app-form-field>
          <app-button
            tipo="submit"
            [texto]="devolucion() ? 'Generar Nuevo Código QR' : 'Generar Código QR de Devolución'"
            [cargando]="generando()"
          />
        </form>

        @if (devolucion(); as dev) {
          <div class="qr-box">
            <h3>Código de Devolución Activo</h3>
            <div class="qr-code">{{ dev.codigoQr }}</div>
            <p>Presenta este código al bibliotecario en la sede receptora para confirmar la entrega.</p>
            <small style="color: var(--color-texto-apoyo);">Si cerraste esta ventana o cambiaste de sede, puedes volver a pulsar "Generar Nuevo Código QR".</small>
          </div>
        }
      </app-card>
    </app-main-template>
  `,
  styles: [`
    .qr-box { text-align: center; padding: 1.5rem; margin-top: 1.25rem; border-top: 1px solid var(--color-borde); }
    .qr-code { display: inline-block; font-family: monospace; font-size: 1.35rem; font-weight: 700; padding: 1rem 1.5rem; border: 2px dashed var(--color-primario); border-radius: var(--radio); background: var(--color-primario-suave); margin: 0.75rem 0; word-break: break-all; }
  `]
})
export class ReturnRequestPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  readonly prestamoId = input.required({ transform: numberAttribute });
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly devolucion = signal<DevolucionDTO | null>(null);
  protected readonly generando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected sedeId = 0;

  ngOnInit(): void {
    this.catalogo.listarSedesActivas().subscribe((s) => {
      this.sedes.set(s);
      this.sedeId = s[0]?.id ?? 0;
    });
  }

  protected async generarNuevoQr(): Promise<void> {
    this.generando.set(true);
    this.error.set(null);
    const pId = this.prestamoId();
    const uId = this.auth.usuarioId()!;
    const sId = Number(this.sedeId);

    try {
      // Intenta crear una nueva solicitud de devolución en el backend
      const nueva = await firstValueFrom(this.ops.solicitarDevolucion(pId, uId, sId));
      this.devolucion.set(nueva);
      this.ui.mostrar('Nuevo código QR generado correctamente.', 'exito');
    } catch (e) {
      // Si ya existía una devolución PENDIENTE para este préstamo, generamos un nuevo QR y la actualizamos
      try {
        const todas = await firstValueFrom(this.ops.listarDevoluciones());
        const existente = todas.find((d) => d.prestamoId === pId && d.estado === 'PENDIENTE');
        if (existente) {
          const nuevoCodigo = 'DEV-' + (typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : Date.now() + '-' + Math.random().toString(36).slice(2, 8).toUpperCase());
          const actualizada = await firstValueFrom(
            this.ops.actualizarDevolucion(existente.id, {
              ...existente,
              codigoQr: nuevoCodigo,
              sedeDevolucionId: sId,
              fechaSolicitud: new Date().toISOString().slice(0, 19),
            })
          );
          this.devolucion.set(actualizada);
          this.ui.mostrar('Se ha generado un nuevo código QR para tu devolución.', 'exito');
        } else {
          this.error.set(extraerMensajeError(e));
        }
      } catch (err2) {
        this.error.set(extraerMensajeError(err2));
      }
    } finally {
      this.generando.set(false);
    }
  }
}
