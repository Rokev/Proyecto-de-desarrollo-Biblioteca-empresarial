import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EjemplarDTO, PrestamoDetalleDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
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
  imports: [FormsModule, RouterLink, PanelTemplate, Card, FormField, Button, Alert],
  template: `
    <app-panel-template titulo="Confirmar Devolución en Mostrador">
      @if (error()) { <app-alert titulo="Error en devolución" [mensaje]="error()!" tipo="error" /> }
      <app-card>
        <form (ngSubmit)="confirmarQr()">
          <app-form-field etiqueta="Código QR de Devolución" para="qr" [obligatorio]="true">
            <input id="qr" class="campo" [(ngModel)]="codigoQr" name="qr" placeholder="DEV-XXXX..." required />
          </app-form-field>
          <app-button tipo="submit" texto="Validar y Confirmar Devolución" />
        </form>

        @if (detalle(); as d) {
          <div class="resultado">
            <app-alert
              titulo="Devolución registrada exitosamente"
              [mensaje]="'Libro: ' + d.libroTitulo + ' · Préstamo #' + d.prestamoId + ' · Sede original: ' + d.sedeNombre + ' · Sede receptora: ' + nombreSedeActual()"
              tipo="exito"
            >
              @if (d.sedeId !== auth.perfil()?.sedeId) {
                <p style="margin:0.4rem 0 0; font-weight:700;">
                  Devolución en sede distinta: el stock fue reasignado a tu sede y se notificó a {{ d.sedeNombre }}.
                  <a routerLink="/notificaciones" style="color:inherit; text-decoration:underline;">Ver notificaciones</a>
                </p>
              }
            </app-alert>

            <h4>Stock actual de "{{ d.libroTitulo }}" por sede</h4>
            <div class="tabla-responsive" style="margin-bottom:1rem;">
              <table class="tabla">
                <thead><tr><th>Sede</th><th>Disponibles</th></tr></thead>
                <tbody>
                  @for (s of sedes(); track s.id) {
                    <tr><td>{{ s.nombre }} ({{ s.ciudad }})</td><td>{{ contarStock(d.libroId, s.id) }}</td></tr>
                  }
                </tbody>
              </table>
            </div>

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
export class ReturnConfirmationPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly catalogo = inject(CatalogoService);
  protected readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected codigoQr = '';
  protected obsNovedad = '';
  protected readonly detalle = signal<PrestamoDetalleDTO | null>(null);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly ejemplares = signal<EjemplarDTO[]>([]);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.catalogo.listarTodasSedes().subscribe((s) => this.sedes.set(s));
    this.catalogo.listarEjemplares().subscribe((e) => this.ejemplares.set(e));
  }

  protected nombreSedeActual(): string {
    const sid = this.auth.perfil()?.sedeId;
    return this.sedes().find((s) => s.id === sid)?.nombre ?? `Sede #${sid}`;
  }

  protected contarStock(libroId: number, sedeId: number): number {
    return this.ejemplares().filter((e) => e.libroId === libroId && e.sedeId === sedeId && e.estado === 'DISPONIBLE').length;
  }

  protected confirmarQr(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId) {
      this.error.set('Tu cuenta de bibliotecario no tiene una sede asignada.');
      return;
    }
    this.error.set(null);
    this.ops.confirmarDevolucionQr(this.codigoQr.trim(), sedeId, this.auth.usuarioId()!).subscribe({
      next: (res) => {
        this.detalle.set(res);
        this.catalogo.listarEjemplares().subscribe((e) => this.ejemplares.set(e));
        this.ui.mostrar('Stock reasignado a la sede receptora.', 'exito');
      },
      error: (e: unknown) => {
        const msg = extraerMensajeError(e);
        if (msg.toLowerCase().includes('interciudad')) {
          this.error.set('Este código corresponde a un pedido interciudad. Debes gestionar su devolución desde la tabla de Pedidos Interciudad en /panel/prestamos.');
        } else {
          this.error.set(msg);
        }
      },
    });
  }

  protected novedad(prestamoId: number, tipo: 'DANADO' | 'PERDIDO'): void {
    this.ops.registrarDanioPerdida(prestamoId, tipo, this.auth.usuarioId()!, this.obsNovedad).subscribe({
      next: () => {
        this.catalogo.listarEjemplares().subscribe((e) => this.ejemplares.set(e));
        this.ui.mostrar(`Ejemplar marcado como ${tipo} y multa aplicada.`, 'advertencia');
      },
      error: (e: unknown) => this.error.set(extraerMensajeError(e)),
    });
  }
}
