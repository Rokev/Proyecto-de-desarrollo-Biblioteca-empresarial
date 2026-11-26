import { CurrencyPipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LibroDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-intercity-order-page',
  imports: [FormsModule, CurrencyPipe, MainTemplate, Card, Alert, FormField, Button],
  template: `
    <app-main-template titulo="Nuevo Pedido Interciudad" subtitulo="Beneficio exclusivo para suscriptores: recibe libros de otras ciudades en tu domicilio.">
      @if (error()) { <app-alert titulo="Error en pedido" [mensaje]="error()!" tipo="error" /> }
      <app-card>
        <form (ngSubmit)="crearPedido()">
          <app-form-field etiqueta="Libro a solicitar" para="lib" [obligatorio]="true">
            <select id="lib" class="campo" [(ngModel)]="libroSeleccionado" name="lib">
              @for (l of libros(); track l.id) { <option [ngValue]="l.id">{{ l.titulo }}</option> }
            </select>
          </app-form-field>
          <app-form-field etiqueta="Ciudad de entrega" para="ciu" [obligatorio]="true">
            <input id="ciu" class="campo" [(ngModel)]="ciudadEntrega" name="ciu" placeholder="Ej. Armenia" required />
          </app-form-field>
          <app-form-field etiqueta="Dirección de entrega a domicilio" para="dir" [obligatorio]="true">
            <input id="dir" class="campo" [(ngModel)]="direccionEntrega" name="dir" placeholder="Calle 12 # 8-45" required />
          </app-form-field>
          <!-- Prueba 54: opción vacía obligatoria en sede de devolución -->
          <app-form-field etiqueta="Sede donde devolverás el libro" para="sedeDev" [obligatorio]="true">
            <select id="sedeDev" class="campo" [(ngModel)]="sedeDevolucionId" name="sedeDev" (change)="cotizar()">
              <option [ngValue]="0" disabled>Seleccione una sede activa...</option>
              @for (s of sedes(); track s.id) { <option [ngValue]="s.id">{{ s.nombre }} ({{ s.ciudad }})</option> }
            </select>
          </app-form-field>
          <!-- Prueba 55: mínimo hoy y máximo 7 días -->
          <app-form-field etiqueta="Fecha de recepción (dentro de los próximos 7 días)" para="fec" [obligatorio]="true">
            <input id="fec" type="date" class="campo" [(ngModel)]="fechaRecepcion" name="fec" [min]="minFecha" [max]="maxFecha" required />
          </app-form-field>
          <app-form-field etiqueta="Hora de recepción" para="hor" [obligatorio]="true">
            <input id="hor" type="time" class="campo" [(ngModel)]="horaRecepcion" name="hor" required />
          </app-form-field>

          @if (tarifaCotizada() !== null) {
            <app-alert
              titulo="Costo de envío interciudad"
              [mensaje]="'Tarifa calculada: ' + (tarifaCotizada() | currency:'COP':'symbol-narrow':'1.0-0')"
              tipo="info"
            />
          }

          <div class="btns">
            <app-button texto="Cotizar Envío" variante="contorno" (presionado)="cotizar()" />
            <app-button tipo="submit" texto="Continuar al Pago" />
          </div>
        </form>
      </app-card>
    </app-main-template>
  `,
  styles: [`.btns { display: flex; gap: 0.75rem; margin-top: 1rem; }`]
})
export class InterCityOrderPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly libroId = input<string>();
  protected readonly libros = signal<LibroDTO[]>([]);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly tarifaCotizada = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  protected libroSeleccionado = 0;
  protected ciudadEntrega = '';
  protected direccionEntrega = '';
  protected sedeDevolucionId = 0;
  protected fechaRecepcion = '';
  protected horaRecepcion = '15:00';

  protected readonly minFecha = new Date().toISOString().slice(0, 10);
  protected readonly maxFecha = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);

  ngOnInit(): void {
    this.catalogo.buscarLibros({ disponible: true }).subscribe((l) => {
      this.libros.set(l);
      this.libroSeleccionado = this.libroId() ? Number(this.libroId()) : (l[0]?.id ?? 0);
    });
    this.catalogo.listarSedesActivas().subscribe((s) => this.sedes.set(s));
  }

  protected cotizar(): void {
    const sede = this.sedes().find((s) => s.id === Number(this.sedeDevolucionId));
    if (!this.ciudadEntrega || !sede) return;
    this.error.set(null);
    this.ops.cotizarEnvio(sede.ciudad, this.ciudadEntrega.trim()).subscribe({
      next: (r) => this.tarifaCotizada.set(r.costoEnvio),
      error: (e) => this.error.set(extraerMensajeError(e)),
    });
  }

  protected crearPedido(): void {
    if (!this.direccionEntrega.trim()) {
      this.error.set('La dirección de entrega es obligatoria.');
      return;
    }
    if (!this.sedeDevolucionId || Number(this.sedeDevolucionId) === 0) {
      this.error.set('Debes seleccionar una sede de devolución activa.');
      return;
    }
    if (!this.fechaRecepcion || this.fechaRecepcion < this.minFecha || this.fechaRecepcion > this.maxFecha) {
      this.error.set('La fecha de recepción debe estar dentro de los próximos 7 días.');
      return;
    }
    this.error.set(null);
    this.ops.solicitarPedidoInterciudad({
      usuarioId: this.auth.usuarioId()!,
      libroId: Number(this.libroSeleccionado),
      ciudadEntrega: this.ciudadEntrega.trim(),
      direccionEntrega: this.direccionEntrega.trim(),
      sedeDevolucionId: Number(this.sedeDevolucionId),
      fechaRecepcion: this.fechaRecepcion,
      horaRecepcion: this.horaRecepcion,
    }).subscribe({
      next: (pedido) => {
        this.router.navigate(['/pago', 'ENVIO_INTERCIUDAD', pedido.id], {
          queryParams: { monto: pedido.costoEnvio },
        });
      },
      error: (e) => this.error.set(extraerMensajeError(e)),
    });
  }
}
