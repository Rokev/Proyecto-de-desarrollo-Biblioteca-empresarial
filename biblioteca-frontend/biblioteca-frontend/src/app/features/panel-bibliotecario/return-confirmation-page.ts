import { Component, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DevolucionDTO, EjemplarDTO, SedeDTO } from '../../core/models/biblioteca.models';
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

        <div class="novedad">
          <h4>¿El libro llegó dañado o se reporta perdido?</h4>
          <p class="ayuda">Usa el mismo código QR. En lugar de confirmar la devolución normal, se registra la novedad, se aplica la multa y el ejemplar sale del stock.</p>
          <input class="campo" placeholder="Observaciones..." [(ngModel)]="obsNovedad" name="obs" />
          <div class="btns">
            <app-button texto="Registrar Dañado" variante="peligro" tamano="pequeno" (presionado)="novedad('DANADO')" />
            <app-button texto="Registrar Perdido" variante="peligro" tamano="pequeno" (presionado)="novedad('PERDIDO')" />
          </div>
        </div>

        @if (resultado(); as r) {
          <div class="resultado">
            <app-alert [titulo]="r.titulo" [mensaje]="r.mensaje" [tipo]="r.tipo">
              @if (r.notificado) {
                <p style="margin:0.4rem 0 0; font-weight:700;">
                  <a routerLink="/notificaciones" style="color:inherit; text-decoration:underline;">Ver notificaciones</a>
                </p>
              }
            </app-alert>

            @if (r.libroId) {
              <h4>Stock actual de "{{ r.libroTitulo }}" por sede</h4>
              <div class="tabla-responsive" style="margin-bottom:1rem;">
                <table class="tabla">
                  <thead><tr><th>Sede</th><th>Disponibles</th><th>En tránsito</th></tr></thead>
                  <tbody>
                    @for (s of sedes(); track s.id) {
                      <tr>
                        <td>{{ s.nombre }} ({{ s.ciudad }})</td>
                        <td>{{ contar(r.libroId, s.id, 'DISPONIBLE') }}</td>
                        <td>{{ contar(r.libroId, s.id, 'EN_TRANSITO') }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>
        }
      </app-card>
    </app-panel-template>
  `,
  styles: [`
    .resultado { margin-top: 1.5rem; }
    .novedad { margin-top: 1.5rem; background: #f8fafc; padding: 1rem; border-radius: var(--radio); border: 1px solid var(--color-borde); display: flex; flex-direction: column; gap: 0.5rem; }
    .novedad h4 { margin: 0; } .ayuda { margin: 0; font-size: 0.85rem; color: var(--color-texto-apoyo); }
    .btns { display: flex; gap: 0.5rem; }
  `]
})
export class ReturnConfirmationPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly catalogo = inject(CatalogoService);
  protected readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected codigoQr = '';
  protected obsNovedad = '';
  protected readonly resultado = signal<Resultado | null>(null);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly ejemplares = signal<EjemplarDTO[]>([]);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.catalogo.listarTodasSedes().subscribe((s) => this.sedes.set(s));
    this.recargarEjemplares();
  }

  private recargarEjemplares(): void {
    this.catalogo.listarEjemplares().subscribe((e) => this.ejemplares.set(e));
  }

  private nombreSede(id: number | undefined): string {
    return this.sedes().find((s) => s.id === id)?.nombre ?? `Sede #${id}`;
  }

  protected contar(libroId: number, sedeId: number, estado: string): number {
    return this.ejemplares().filter((e) => e.libroId === libroId && e.sedeId === sedeId && e.estado === estado).length;
  }

  protected confirmarQr(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId) {
      this.error.set('Tu cuenta de bibliotecario no tiene una sede asignada.');
      return;
    }
    this.error.set(null);
    this.resultado.set(null);
    this.ops.confirmarDevolucionQr(this.codigoQr.trim(), this.auth.usuarioId()!).subscribe({
      next: (dev) => this.mostrarConfirmacion(dev),
      error: (e: unknown) => {
        const msg = extraerMensajeError(e);
        this.error.set(msg.toLowerCase().includes('interciudad')
          ? 'Este código corresponde a un pedido interciudad. Debes gestionar su devolución desde la tabla de Pedidos Interciudad en /panel/prestamos.'
          : msg);
      },
    });
  }

  /** El backend devuelve solo la devolución; se completan libro y ejemplar con el préstamo. */
  private mostrarConfirmacion(dev: DevolucionDTO): void {
    this.ops.obtenerPrestamo(dev.prestamoId).subscribe({
      next: (prestamo) => {
        forkJoin({
          ejemplares: this.catalogo.listarEjemplares(),
        }).subscribe(({ ejemplares }) => {
          this.ejemplares.set(ejemplares);
          const ej = ejemplares.find((x) => x.id === prestamo.ejemplarId);
          if (!ej) {
            this.resultado.set({ titulo: 'Devolución registrada', mensaje: `Préstamo #${dev.prestamoId} devuelto.`, tipo: 'exito', libroId: 0, libroTitulo: '', notificado: false });
            return;
          }
          this.catalogo.obtenerLibro(ej.libroId).subscribe({
            next: (libro) => this.armarResultado(dev, ej, libro.titulo),
            error: () => this.armarResultado(dev, ej, `Libro #${ej.libroId}`),
          });
        });
      },
      error: () => this.resultado.set({ titulo: 'Devolución registrada', mensaje: `Préstamo #${dev.prestamoId} devuelto.`, tipo: 'exito', libroId: 0, libroTitulo: '', notificado: false }),
    });
  }

  private armarResultado(dev: DevolucionDTO, ej: EjemplarDTO, titulo: string): void {
    const sedeRecibe = this.nombreSede(dev.sedeDevolucionId);
    const origen = this.nombreSede(ej.sedeId);
    const enOrigen = ej.sedeId === dev.sedeDevolucionId;
    const base = `Libro: ${titulo} · Préstamo #${dev.prestamoId} · Recibido en: ${sedeRecibe}`;
    const mensaje = enOrigen
      ? `${base}. El ejemplar quedó DISPONIBLE en esta sede.`
      : `${base}. El ejemplar pertenece a ${origen}: quedó EN TRÁNSITO de regreso y se notificó a esa sede. Volverá a contar como disponible cuando llegue.`;
    this.resultado.set({ titulo: 'Devolución confirmada', mensaje, tipo: 'exito', libroId: ej.libroId, libroTitulo: titulo, notificado: !enOrigen });
    this.ui.mostrar(enOrigen ? 'Devolución confirmada: ejemplar disponible.' : 'Devolución confirmada: ejemplar en tránsito a su sede de origen.', 'exito');
  }

  protected novedad(tipo: 'DANADO' | 'PERDIDO'): void {
    const qr = this.codigoQr.trim();
    if (!qr) {
      this.error.set('Ingresa el código QR de la devolución para registrar la novedad.');
      return;
    }
    this.error.set(null);
    this.resultado.set(null);
    // Se busca la devolución por su código QR para obtener el préstamo.
    this.ops.listarDevoluciones().subscribe({
      next: (todas) => {
        const dev = todas.find((d) => d.codigoQr === qr);
        if (!dev) { this.error.set('No existe una devolución con ese código QR.'); return; }
        this.ops.registrarDanioPerdida(dev.prestamoId, tipo, this.auth.usuarioId()!, this.obsNovedad || undefined).subscribe({
          next: () => {
            this.recargarEjemplares();
            const txt = tipo === 'DANADO' ? 'dañado' : 'perdido';
            this.resultado.set({ titulo: 'Novedad registrada', mensaje: `Préstamo #${dev.prestamoId}: el ejemplar quedó como ${txt}, salió del stock y se aplicó la multa.`, tipo: 'advertencia', libroId: 0, libroTitulo: '', notificado: false });
            this.ui.mostrar(`Ejemplar marcado como ${txt} y multa aplicada.`, 'advertencia');
          },
          error: (e: unknown) => this.error.set(extraerMensajeError(e)),
        });
      },
      error: (e: unknown) => this.error.set(extraerMensajeError(e)),
    });
  }
}

interface Resultado {
  titulo: string;
  mensaje: string;
  tipo: 'exito' | 'advertencia';
  libroId: number;
  libroTitulo: string;
  notificado: boolean;
}