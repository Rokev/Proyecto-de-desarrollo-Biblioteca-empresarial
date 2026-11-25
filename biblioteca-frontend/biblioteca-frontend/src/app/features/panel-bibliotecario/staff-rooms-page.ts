import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DisponibilidadSalasDTO, SalaEstudioDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-staff-rooms-page',
  imports: [FormsModule, CurrencyPipe, PanelTemplate, Card, Badge, Button, FormField, ConfirmDialog],
  template: `
    <app-panel-template titulo="Gestión de Salas de Estudio de la Sede">
      <app-card>
        <h3>{{ editandoId() ? 'Editar Sala #' + editandoId() : 'Crear Nueva Sala' }}</h3>
        <form (ngSubmit)="guardar()" class="form-grid">
          @if (editandoId()) {
            <app-form-field etiqueta="Identificador (no editable)" para="sId">
              <input id="sId" class="campo" [value]="editandoId()" disabled />
            </app-form-field>
          }
          <app-form-field etiqueta="Nombre de la sala" para="sNom" [obligatorio]="true">
            <input id="sNom" class="campo" [(ngModel)]="form.nombre" name="nom" required />
          </app-form-field>
          <app-form-field etiqueta="Capacidad" para="sCap" [obligatorio]="true">
            <input id="sCap" type="number" min="1" class="campo" [(ngModel)]="form.capacidad" name="cap" required />
          </app-form-field>
          <app-form-field etiqueta="Tarifa por hora (COP)" para="sTar" [obligatorio]="true">
            <input id="sTar" type="number" min="0" class="campo" [(ngModel)]="form.tarifaPorHora" name="tar" required />
          </app-form-field>
          <app-form-field etiqueta="Descripción" para="sDesc">
            <input id="sDesc" class="campo" [(ngModel)]="form.descripcion" name="desc" />
          </app-form-field>
          <app-form-field etiqueta="¿Sala activa?" para="sActiva">
            <select id="sActiva" class="campo" name="activa" [(ngModel)]="form.activa">
              <option [ngValue]="true">Sí, activa</option>
              <option [ngValue]="false">No, inactiva</option>
            </select>
          </app-form-field>
          <div style="align-self:end; margin-bottom:0.9rem; display:flex; gap:0.5rem;">
            <app-button tipo="submit" [texto]="editandoId() ? 'Actualizar Sala' : 'Crear Sala'" />
            @if (editandoId()) { <app-button texto="Cancelar" variante="contorno" (presionado)="reset()" /> }
          </div>
        </form>
      </app-card>

      <app-card style="margin-top:1.5rem;">
        <h3>Consultar Disponibilidad por Franja Horaria</h3>
        <div style="display:flex; gap:0.75rem; flex-wrap:wrap; align-items:flex-end;">
          <label>Inicio: <input type="datetime-local" class="campo" [(ngModel)]="inicio" /></label>
          <label>Fin: <input type="datetime-local" class="campo" [(ngModel)]="fin" /></label>
          <app-button texto="Consultar" variante="contorno" (presionado)="consultarDisp()" />
        </div>
        @if (disp(); as d) {
          <p style="margin-top:0.75rem;"><strong>Libres:</strong> {{ d.salasDisponibles }} de {{ d.totalSalas }}</p>
        }
      </app-card>

      <div class="tabla-responsive" style="margin-top:1.5rem;">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Nombre</th><th>Capacidad</th><th>Tarifa/Hora</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (s of salas(); track s.id) {
              <tr>
                <td>#{{ s.id }}</td>
                <td><strong>{{ s.nombre }}</strong></td>
                <td>{{ s.capacidad }} personas</td>
                <td>{{ s.tarifaPorHora | currency:'COP':'symbol-narrow':'1.0-0' }}</td>
                <td><app-badge [texto]="s.activa ? 'Activa' : 'Inactiva'" [estado]="s.activa ? 'ACTIVA' : 'VENCIDO'" /></td>
                <td style="display:flex; gap:0.4rem;">
                  <app-button texto="Editar" variante="contorno" tamano="pequeno" (presionado)="editar(s)" />
                  <app-button texto="Editar" variante="contorno" tamano="pequeno" (presionado)="editar(s)" />
                  <app-button [texto]="s.activa ? 'Desactivar' : 'Activar'" tamano="pequeno" variante="secundario" (presionado)="alternarActiva(s)" />
                  <app-button texto="Eliminar" variante="peligro" tamano="pequeno" (presionado)="pedirEliminar(s)" />
                  <app-button texto="Eliminar" variante="peligro" tamano="pequeno" (presionado)="pedirEliminar(s)" />
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        titulo="Eliminar Sala de Estudio"
        [mensaje]="'¿Confirmas eliminar la sala ' + (salaSel()?.nombre || '') + '? Si tiene reservas activas la operación será bloqueada.'"
        [peligro]="true"
        (confirmar)="ejecutarEliminar()"
      />
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 0.75rem; }`]
})
export class StaffRoomsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly salas = signal<SalaEstudioDTO[]>([]);
  protected readonly disp = signal<DisponibilidadSalasDTO | null>(null);
  protected readonly editandoId = signal<number | null>(null);
  protected form: Partial<SalaEstudioDTO> = { nombre: '', capacidad: 4, tarifaPorHora: 10000, descripcion: '', activa: true };
  protected inicio = ''; protected fin = '';

  protected readonly confirmOpen = signal(false);
  protected readonly salaSel = signal<SalaEstudioDTO | null>(null);

  ngOnInit(): void { this.cargar(); this.consultarDisp(); }

  private cargar(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    this.ops.listarSalas().subscribe((all) =>
      this.salas.set(sedeId ? all.filter((s) => s.sedeId === sedeId) : all)
    );
  }

  protected consultarDisp(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!sedeId) return;
    this.ops.disponibilidadSalas(sedeId, this.inicio || undefined, this.fin || undefined).subscribe((d) => this.disp.set(d));
  }

  protected editar(s: SalaEstudioDTO): void { this.editandoId.set(s.id); this.form = { ...s }; }
  protected reset(): void { this.editandoId.set(null); this.form = { nombre: '', capacidad: 4, tarifaPorHora: 10000, descripcion: '', activa: true }; }

  protected guardar(): void {
    const sedeId = this.auth.perfil()?.sedeId;
    if (!this.form.nombre?.trim() || !sedeId) return;
    const payload = { ...this.form, sedeId };
    const id = this.editandoId();
    (id ? this.ops.actualizarSala(id, payload) : this.ops.crearSala(payload)).subscribe({
      next: () => { this.ui.mostrar(id ? 'Sala actualizada.' : 'Sala creada.', 'exito'); this.reset(); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected alternarActiva(s: SalaEstudioDTO): void {
    this.ops.actualizarSala(s.id, { ...s, activa: !s.activa }).subscribe({
      next: () => {
        this.ui.mostrar(`Sala ${s.activa ? 'desactivada' : 'activada'}.`, 'exito');
        this.cargar();
      },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
  
  protected pedirEliminar(s: SalaEstudioDTO): void { this.salaSel.set(s); this.confirmOpen.set(true); }
  protected ejecutarEliminar(): void {
    const s = this.salaSel(); if (!s) return;
    this.ops.eliminarSala(s.id).subscribe({
      next: () => { this.ui.mostrar('Sala eliminada.', 'info'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
