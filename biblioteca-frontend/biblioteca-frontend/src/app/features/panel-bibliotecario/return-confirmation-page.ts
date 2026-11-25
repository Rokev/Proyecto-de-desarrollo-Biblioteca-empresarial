import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PrestamoDetalleDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-return-confirmation-page',
  imports: [FormsModule, PanelTemplate, Card, FormField, Button, Alert],
  template: `
    <app-panel-template titulo="Confirmar Devolución en Mostrador">
      @if (error()) { <app-alert titulo="Error" [mensaje]="error()!" tipo="error" /> }
      <app-card>
        <form (ngSubmit)="confirmarQr()">
          <app-form-field etiqueta="Código QR de Devolución" para="qr" [obligatorio]="true">
            <input id="qr" class="campo" [(ngModel)]="codigoQr" name="qr" placeholder="DEV-XXXX..." required />
          </app-form-field>
          <app-button tipo="submit" texto="Validar y Confirmar Devolución" />
        </form>

        @if (detalle(); as d) {
          <div class="resultado">
            <app-alert titulo="Devolución registrada exitosamente" [mensaje]="'Libro: ' + d.libroTitulo + ' (Préstamo #' + d.prestamoId + ')'" tipo="exito" />
            <div class="novedad">
              <h4>¿El libro presenta daños o se reporta perdido?</h4>
              <input class="campo" placeholder="Observaciones..." [(ngModel)]="obsNovedad" />
              <div class="btns">
                <app-button texto="Registrar Dañado" variante="peligro" tamano="pequeno" (presionado)="novedad(d.prestamoId, 'DANADO')" />
                <app-button texto="Registrar Perdido" variante="peligro" tamano="pequeno" (presionado)="novedad(d.prestamoId, 'PERDIDO')" />
              </div>
            </div>
          </div>
        }
      </app-card>
    </app-panel-template>
  `,
  styles: [`.resultado { margin-top: 1.5rem; } .novedad { background: #f8fafc; padding: 1rem; border-radius: var(--radio); border: 1px solid var(--color-borde); display: flex; flex-direction: column; gap: 0.5rem; } .btns { display: flex; gap: 0.5rem; }`]
})
export class ReturnConfirmationPage {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected codigoQr = '';
  protected obsNovedad = '';
  protected readonly detalle = signal<PrestamoDetalleDTO | null>(null);
  protected readonly error = signal<string | null>(null);

  protected confirmarQr(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId) {
      this.error.set('Tu cuenta de bibliotecario no tiene una sede asignada.');
      return;
    }
    const bibId = this.auth.usuarioId()!;
    this.error.set(null);
    this.ops.confirmarDevolucionQr(this.codigoQr.trim(), sedeId, bibId).subscribe({
      next: (res) => {
        this.detalle.set(res);
        this.ui.mostrar('Stock reasignado a la sede receptora.', 'exito');
      },
      error: (e) => this.error.set(extraerMensajeError(e)),
    });
  }

  protected novedad(prestamoId: number, tipo: 'DANADO' | 'PERDIDO'): void {
    this.ops.registrarDanioPerdida(prestamoId, tipo, this.auth.usuarioId()!, this.obsNovedad).subscribe({
      next: () => this.ui.mostrar(`Ejemplar marcado como ${tipo} y multa aplicada.`, 'advertencia'),
      error: (e) => this.error.set(extraerMensajeError(e)),
    });
  }
}
