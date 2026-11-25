import { Component, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DevolucionDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-return-request-page',
  imports: [FormsModule, MainTemplate, Card, FormField, Button],
  template: `
    <app-main-template titulo="Devolver Libro" subtitulo="Puedes devolver préstamos normales en cualquier sede activa de la red.">
      <app-card>
        @if (!devolucion()) {
          <form (ngSubmit)="generarQr()">
            <app-form-field etiqueta="Selecciona la sede donde entregarás el libro" para="sedeDev" [obligatorio]="true">
              <select id="sedeDev" class="campo" [(ngModel)]="sedeId" name="sedeDev">
                @for (s of sedes(); track s.id) {
                  <option [ngValue]="s.id">{{ s.nombre }} ({{ s.ciudad }})</option>
                }
              </select>
            </app-form-field>
            <app-button tipo="submit" texto="Generar Código QR de Devolución" />
          </form>
        } @else {
          <div class="qr-box">
            <h3>Código de Devolución Generado</h3>
            <div class="qr-code">{{ devolucion()!.codigoQr }}</div>
            <p>Presenta este código al bibliotecario en la sede receptora para confirmar la entrega.</p>
          </div>
        }
      </app-card>
    </app-main-template>
  `,
  styles: [`
    .qr-box { text-align: center; padding: 1.5rem; }
    .qr-code { display: inline-block; font-family: monospace; font-size: 1.4rem; font-weight: 700; padding: 1rem 1.5rem; border: 2px dashed var(--color-primario); border-radius: var(--radio); background: var(--color-primario-suave); margin: 0.75rem 0; }
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
  protected sedeId = 0;

  ngOnInit(): void {
    this.catalogo.listarSedesActivas().subscribe((s) => {
      this.sedes.set(s);
      this.sedeId = s[0]?.id ?? 0;
    });
  }

  protected generarQr(): void {
    this.ops.solicitarDevolucion(this.prestamoId(), this.auth.usuarioId()!, Number(this.sedeId)).subscribe({
      next: (d) => this.devolucion.set(d),
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
