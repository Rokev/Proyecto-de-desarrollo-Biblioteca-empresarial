import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AsistenteEventoDTO, EventoDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-staff-events-page',
  imports: [FormsModule, DatePipe, PanelTemplate, Card, Badge, Button, FormField, ConfirmDialog, Alert],
  template: `
    <app-panel-template titulo="Gestión de Eventos de la Sede">
      <app-card>
        <h3>{{ editandoId() ? 'Editar Evento #' + editandoId() : 'Crear Nuevo Evento' }}</h3>
        @if (editandoId()) {
          <app-alert titulo="Aviso" mensaje="Al modificar fecha u hora se notificará automáticamente a los asistentes inscritos." tipo="info" />
        }
        <form (ngSubmit)="guardar()" novalidate>
          <div class="form-grid">
            <app-form-field etiqueta="Título" para="evTit" [obligatorio]="true" [error]="errs()['titulo'] || null">
              <input id="evTit" class="campo" [(ngModel)]="form.titulo" name="tit" />
            </app-form-field>
            <app-form-field etiqueta="Cupo Máximo" para="evCup" [obligatorio]="true" [error]="errs()['cupo'] || null">
              <input id="evCup" type="number" min="1" class="campo" [(ngModel)]="form.cupoMaximo" name="cup" />
            </app-form-field>
            <app-form-field etiqueta="Fecha y Hora Inicio" para="evIni" [obligatorio]="true" [error]="errs()['inicio'] || null">
              <input id="evIni" type="datetime-local" class="campo" [(ngModel)]="form.fechaInicio" name="ini" />
            </app-form-field>
            <app-form-field etiqueta="Fecha y Hora Fin" para="evFin" [obligatorio]="true" [error]="errs()['fin'] || null">
              <input id="evFin" type="datetime-local" class="campo" [(ngModel)]="form.fechaFin" name="fin" />
            </app-form-field>
          </div>
          <app-form-field etiqueta="Descripción" para="evDesc" [obligatorio]="true" [error]="errs()['desc'] || null">
            <textarea id="evDesc" class="campo" rows="2" [(ngModel)]="form.descripcion" name="desc"></textarea>
          </app-form-field>
          <label style="display:flex; align-items:center; gap:0.5rem; margin-bottom:1rem; font-weight:700;">
            <input type="checkbox" [(ngModel)]="form.exclusivo" name="exc" /> Evento exclusivo para suscriptores
          </label>
          <div style="display:flex; gap:0.6rem;">
            <app-button tipo="submit" [texto]="editandoId() ? 'Guardar Cambios' : 'Publicar Evento'" />
            @if (editandoId()) { <app-button texto="Cancelar" variante="contorno" (presionado)="reset()" /> }
          </div>
        </form>
      </app-card>

      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>Título</th><th>Inicio</th><th>Cupos (Disp / Max)</th><th>Tipo</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (ev of eventos(); track ev.id) {
              <tr>
                <td><strong>{{ ev.titulo }}</strong></td>
                <td>{{ ev.fechaInicio | date:'d MMM y, h:mm a' }}</td>
                <td>{{ ev.cuposDisponibles }} / {{ ev.cupoMaximo }}</td>
                <td><app-badge [texto]="ev.exclusivo ? 'Exclusivo' : 'General'" [variante]="ev.exclusivo ? 'advertencia' : 'info'" /></td>
                <td><app-badge [texto]="ev.estado" [estado]="ev.estado" /></td>
                <td style="display:flex; gap:0.35rem; flex-wrap:wrap;">
                  <app-button texto="Editar" variante="contorno" tamano="pequeno" (presionado)="editar(ev)" />
                  <app-button texto="Cupos" variante="contorno" tamano="pequeno" (presionado)="abrirCupos(ev)" />
                  <app-button texto="Asistentes" variante="secundario" tamano="pequeno" (presionado)="verAsistentes(ev)" />
                  @if (ev.cupoMaximo === ev.cuposDisponibles) {
                    <app-button texto="Eliminar" variante="peligro" tamano="pequeno" (presionado)="pedirAccion(ev, 'ELIMINAR')" />
                  } @else if (ev.estado !== 'CANCELADO') {
                    <app-button texto="Cancelar" variante="peligro" tamano="pequeno" (presionado)="pedirAccion(ev, 'CANCELAR')" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Modal Modificar Cupos (Pruebas 84 y 85) -->
      @if (eventoCupos(); as ec) {
        <div class="backdrop" (click)="eventoCupos.set(null)">
          <div class="dialog" (click)="$event.stopPropagation()">
            <h3>Modificar Cupos · {{ ec.titulo }}</h3>
            <p>Reservas confirmadas actuales: <strong>{{ ec.cupoMaximo - ec.cuposDisponibles }}</strong></p>
            @if (errCupos()) { <app-alert titulo="Error" [mensaje]="errCupos()!" tipo="error" /> }
            <app-form-field etiqueta="Nuevo cupo máximo" para="newCup">
              <input id="newCup" type="number" class="campo" [(ngModel)]="nuevoCupo" />
            </app-form-field>
            <div style="display:flex; justify-content:flex-end; gap:0.5rem;">
              <app-button texto="Cerrar" variante="contorno" (presionado)="eventoCupos.set(null)" />
              <app-button texto="Guardar Cupos" (presionado)="guardarCupos()" />
            </div>
          </div>
        </div>
      }

      <!-- Modal Asistentes (Prueba 86) -->
      @if (asistentesModal(); as lista) {
        <div class="backdrop" (click)="asistentesModal.set(null)">
          <div class="dialog" style="max-width:600px;" (click)="$event.stopPropagation()">
            <h3>Asistentes Inscritos</h3>
            <table class="tabla">
              <thead><tr><th>Nombre</th><th>Correo</th><th>Cupos</th><th>Estado</th></tr></thead>
              <tbody>
                @for (a of lista; track a.inscripcionId) {
                  <tr><td>{{ a.nombre }}</td><td>{{ a.correo }}</td><td>{{ a.cantidadCupos }}</td><td><app-badge [texto]="a.estado" [estado]="a.estado" /></td></tr>
                } @empty {
                  <tr><td colspan="4">Sin asistentes registrados.</td></tr>
                }
              </tbody>
            </table>
            <div style="margin-top:1rem; text-align:right;"><app-button texto="Cerrar" (presionado)="asistentesModal.set(null)" /></div>
          </div>
        </div>
      }

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        [titulo]="accionEv() === 'ELIMINAR' ? 'Eliminar Evento' : 'Cancelar Evento'"
        [mensaje]="mensajeConfirm()"
        [peligro]="true"
        (confirmar)="ejecutarAccion()"
      />
    </app-panel-template>
  `,
  styles: [`
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 0.75rem; }
    .backdrop { position: fixed; inset: 0; background: rgba(15,23,42,.55); display:flex; align-items:center; justify-content:center; z-index:9500; padding:1rem; }
    .dialog { background:#fff; padding:1.5rem; border-radius:var(--radio-lg); max-width:440px; width:100%; }
  `]
})
export class StaffEventsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly eventos = signal<EventoDTO[]>([]);
  protected readonly editandoId = signal<number | null>(null);
  protected readonly errs = signal<Record<string, string>>({});
  protected form: Partial<EventoDTO> = { titulo: '', descripcion: '', fechaInicio: '', fechaFin: '', cupoMaximo: 20, exclusivo: false };

  protected readonly eventoCupos = signal<EventoDTO | null>(null);
  protected nuevoCupo = 0;
  protected readonly errCupos = signal<string | null>(null);

  protected readonly asistentesModal = signal<AsistenteEventoDTO[] | null>(null);
  protected readonly confirmOpen = signal(false);
  protected readonly eventoSel = signal<EventoDTO | null>(null);
  protected readonly accionEv = signal<'ELIMINAR' | 'CANCELAR'>('ELIMINAR');
  protected readonly mensajeConfirm = signal('');

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    this.ops.listarTodosEventos().subscribe((all) =>
      this.eventos.set(sedeId ? all.filter((e) => e.sedeId === sedeId) : all)
    );
  }

  protected editar(ev: EventoDTO): void {
    this.editandoId.set(ev.id);
    this.form = { ...ev, fechaInicio: ev.fechaInicio.slice(0, 16), fechaFin: ev.fechaFin.slice(0, 16) };
  }

  protected reset(): void {
    this.editandoId.set(null); this.errs.set({});
    this.form = { titulo: '', descripcion: '', fechaInicio: '', fechaFin: '', cupoMaximo: 20, exclusivo: false };
  }

  protected guardar(): void {
    const e: Record<string, string> = {};
    if (!this.form.titulo?.trim()) e['titulo'] = 'El título es obligatorio.';
    if (!this.form.descripcion?.trim()) e['desc'] = 'La descripción es obligatoria.';
    if (!this.form.cupoMaximo || this.form.cupoMaximo <= 0) e['cupo'] = 'El cupo debe ser mayor a 0.';
    if (!this.form.fechaInicio) e['inicio'] = 'Fecha de inicio obligatoria.';
    if (!this.form.fechaFin) e['fin'] = 'Fecha de fin obligatoria.';
    if (this.form.fechaInicio && this.form.fechaFin && this.form.fechaFin <= this.form.fechaInicio) {
      e['fin'] = 'La fecha de fin debe ser posterior a la de inicio.';
    }
    this.errs.set(e);
    if (Object.keys(e).length > 0) return;

    const sedeId = this.auth.perfil()?.sedeId;
    const payload: Partial<EventoDTO> = { ...this.form, sedeId: this.form.sedeId || sedeId };
    const id = this.editandoId();
    (id ? this.ops.actualizarEvento(id, payload) : this.ops.crearEvento(payload)).subscribe({
      next: () => { this.ui.mostrar(id ? 'Evento actualizado y asistentes notificados.' : 'Evento publicado.', 'exito'); this.reset(); this.cargar(); },
      error: (err) => this.ui.mostrar(extraerMensajeError(err), 'error'),
    });
  }

  protected abrirCupos(ev: EventoDTO): void {
    this.eventoCupos.set(ev); this.nuevoCupo = ev.cupoMaximo; this.errCupos.set(null);
  }

  protected guardarCupos(): void {
    const ev = this.eventoCupos();
    if (!ev) return;
    const reservadas = ev.cupoMaximo - ev.cuposDisponibles;
    if (this.nuevoCupo < reservadas) {
      this.errCupos.set(`No puedes reducir los cupos por debajo de las reservas confirmadas (${reservadas}).`);
      return;
    }
    this.ops.actualizarEvento(ev.id, { ...ev, cupoMaximo: this.nuevoCupo, cuposDisponibles: this.nuevoCupo - reservadas }).subscribe({
      next: () => { this.ui.mostrar('Cupos actualizados.', 'exito'); this.eventoCupos.set(null); this.cargar(); },
      error: (e) => this.errCupos.set(extraerMensajeError(e)),
    });
  }

  protected verAsistentes(ev: EventoDTO): void {
    this.ops.asistentesEvento(ev.id).subscribe((a) => this.asistentesModal.set(a));
  }

  protected pedirAccion(ev: EventoDTO, tipo: 'ELIMINAR' | 'CANCELAR'): void {
    this.eventoSel.set(ev); this.accionEv.set(tipo);
    const res = ev.cupoMaximo - ev.cuposDisponibles;
    this.mensajeConfirm.set(tipo === 'ELIMINAR'
      ? `¿Eliminar el evento "${ev.titulo}"? No tiene reservas.`
      : `¿Cancelar el evento "${ev.titulo}"? Se liberarán ${res} reservas y se notificará a los asistentes.`);
    this.confirmOpen.set(true);
  }

  protected ejecutarAccion(): void {
    const ev = this.eventoSel();
    if (!ev) return;

    if (this.accionEv() === 'ELIMINAR') {
      this.ops.eliminarEvento(ev.id).subscribe({
        next: () => {
          this.ui.mostrar('Evento eliminado.', 'info');
          this.cargar();
        },
        error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    } else {
      this.ops.cancelarEvento(ev.id).subscribe({
        next: () => {
          this.ui.mostrar('Evento cancelado.', 'info');
          this.cargar();
        },
        error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
      });
    }
  }
}
