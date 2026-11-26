import { Component, computed, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EjemplarDTO, LibroDTO, SancionDTO, SedeDTO } from '../../core/models/biblioteca.models';
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
  selector: 'app-loan-request-page',
  imports: [FormsModule, MainTemplate, Card, Alert, FormField, Button],
  template: `
    <app-main-template titulo="Solicitar Préstamo de Libro">
      @if (sancionesActivas().length > 0) {
        <app-alert
          titulo="Tienes sanciones activas"
          [mensaje]="'Motivo: ' + sancionesActivas()[0].motivo"
          tipo="error"
        />
      }
      @if (error()) { <app-alert titulo="No se pudo procesar" [mensaje]="error()!" tipo="error" /> }

      <app-card>
        <h3>{{ libro()?.titulo }}</h3>
        <form (ngSubmit)="confirmar()">
          <app-form-field etiqueta="Sede de retiro" para="sede" [obligatorio]="true">
            <select id="sede" class="campo" [ngModel]="sedeSeleccionada()" (ngModelChange)="sedeSeleccionada.set($event)" name="sede">
              @for (s of sedes(); track s.id) {
                <option [ngValue]="s.id">{{ s.nombre }} ({{ s.ciudad }})</option>
              }
            </select>
          </app-form-field>

          <app-form-field etiqueta="Modalidad de entrega" para="mod" [obligatorio]="true">
            <select id="mod" class="campo" [(ngModel)]="modalidad" name="mod">
              <option value="SEDE">Retiro en mostrador de sede</option>
              <option value="DOMICILIO">Entrega a domicilio local</option>
            </select>
          </app-form-field>

          @if (modalidad === 'DOMICILIO') {
            <app-form-field etiqueta="Dirección de entrega" para="dir" [obligatorio]="true">
              <input id="dir" class="campo" [(ngModel)]="direccionEntrega" name="dir" required />
            </app-form-field>
          }

          <app-button tipo="submit" texto="Confirmar Solicitud de Préstamo" [cargando]="guardando()" />
        </form>
      </app-card>
    </app-main-template>
  `
})
export class LoanRequestPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);
  private readonly router = inject(Router);

  readonly libroId = input.required({ transform: numberAttribute });
  readonly sedeId = input<string>();

  protected readonly libro = signal<LibroDTO | null>(null);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly ejemplares = signal<EjemplarDTO[]>([]);
  protected readonly sancionesActivas = signal<SancionDTO[]>([]);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly sedeSeleccionada = signal(0);
  protected modalidad: 'SEDE' | 'DOMICILIO' = 'SEDE';
  protected direccionEntrega = '';

  protected readonly ejemplarDisponible = computed(() =>
    this.ejemplares().find(
      (e) => e.libroId === this.libroId() && e.sedeId === Number(this.sedeSeleccionada()) && e.estado === 'DISPONIBLE'
    )
  );

  ngOnInit(): void {
    const uid = this.auth.usuarioId()!;
    this.catalogo.obtenerLibro(this.libroId()).subscribe((l) => this.libro.set(l));
    this.catalogo.listarSedesActivas().subscribe((s) => {
      this.sedes.set(s);
      this.sedeSeleccionada.set(this.sedeId() ? Number(this.sedeId()) : (s[0]?.id ?? 0));
    });
    this.catalogo.listarEjemplares().subscribe((e) => this.ejemplares.set(e));
    this.ops.sancionesUsuario(uid).subscribe((san) =>
      this.sancionesActivas.set(san.filter((x) => x.estado === 'ACTIVA'))
    );
  }

  protected confirmar(): void {
    const ej = this.ejemplarDisponible();
    if (!ej) {
      this.error.set('No hay ejemplares disponibles en la sede seleccionada.');
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.ops.crearPrestamo({
      usuarioId: this.auth.usuarioId()!,
      ejemplarId: ej.id!,
      sedeRetiroId: Number(this.sedeSeleccionada()),
      modalidad: this.modalidad,
      direccionEntrega: this.modalidad === 'DOMICILIO' ? this.direccionEntrega : undefined,
    }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.ui.mostrar('Solicitud de préstamo registrada correctamente.', 'exito');
        this.router.navigate(['/mi-cuenta/prestamos']);
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(extraerMensajeError(e));
      },
    });
  }
}
