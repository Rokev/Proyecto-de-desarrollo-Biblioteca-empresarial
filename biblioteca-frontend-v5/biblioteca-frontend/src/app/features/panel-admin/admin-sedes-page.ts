import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SedeDTO } from '../../core/models/biblioteca.models';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-admin-sedes-page',
  imports: [FormsModule, PanelTemplate, Card, Badge, Button, FormField, ConfirmDialog, Alert],
  template: `
    <app-panel-template titulo="Gestión de Sedes / Bibliotecas">
      @if (errorBloqueo()) {
        <app-alert titulo="No se puede desactivar la sede" [mensaje]="errorBloqueo()!" tipo="error" />
      }

      <app-card>
        <h3>{{ editId() ? 'Editar Sede #' + editId() : 'Registrar Nueva Sede' }}</h3>
        <form (ngSubmit)="guardar()" novalidate>
          <div class="form-grid">
            <app-form-field etiqueta="Nombre" para="sNom" [obligatorio]="true" [error]="errs()['nombre'] || null">
              <input id="sNom" class="campo" [(ngModel)]="form.nombre" name="nom" />
            </app-form-field>
            <app-form-field etiqueta="Ciudad" para="sCiu" [obligatorio]="true" [error]="errs()['ciudad'] || null">
              <input id="sCiu" class="campo" [(ngModel)]="form.ciudad" name="ciu" />
            </app-form-field>
            <app-form-field etiqueta="Dirección" para="sDir" [obligatorio]="true" [error]="errs()['direccion'] || null">
              <input id="sDir" class="campo" [(ngModel)]="form.direccion" name="dir" />
            </app-form-field>
            <app-form-field etiqueta="Teléfono" para="sTel">
              <input id="sTel" class="campo" [(ngModel)]="form.telefono" name="tel" />
            </app-form-field>
            <app-form-field etiqueta="Correo de contacto" para="sCor" [error]="errs()['correo'] || null">
              <input id="sCor" type="email" class="campo" [(ngModel)]="form.correo" name="cor" />
            </app-form-field>
            <app-form-field etiqueta="Hora apertura" para="sAp">
              <input id="sAp" type="time" class="campo" [(ngModel)]="form.horaApertura" name="ap" />
            </app-form-field>
            <app-form-field etiqueta="Hora cierre" para="sCi">
              <input id="sCi" type="time" class="campo" [(ngModel)]="form.horaCierre" name="ci" />
            </app-form-field>
          </div>
          <label style="display:flex; align-items:center; gap:0.5rem; margin-bottom:1rem; font-weight:700;">
            <input type="checkbox" [(ngModel)]="form.aliada" name="ali" /> Sede aliada
          </label>
          <div style="display:flex; gap:0.6rem;">
            <app-button tipo="submit" [texto]="editId() ? 'Guardar Cambios' : 'Crear Sede'" />
            @if (editId()) { <app-button texto="Cancelar" variante="contorno" (presionado)="reset()" /> }
          </div>
        </form>
      </app-card>

      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Nombre</th><th>Ciudad</th><th>Dirección</th><th>Horario</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (s of sedes(); track s.id) {
              <tr>
                <td>{{ s.id }}</td>
                <td><strong>{{ s.nombre }}</strong></td>
                <td>{{ s.ciudad }}</td>
                <td>{{ s.direccion }}</td>
                <td>{{ s.horaApertura || '08:00' }} - {{ s.horaCierre || '20:00' }}</td>
                <td><app-badge [texto]="s.enOperacion ? 'Activa' : 'Fuera de servicio'" [estado]="s.enOperacion ? 'ACTIVA' : 'FUERA_DE_SERVICIO'" /></td>
                <td style="display:flex; gap:0.4rem;">
                  <app-button texto="Editar" variante="contorno" tamano="pequeno" (presionado)="editar(s)" />
                  @if (s.enOperacion) {
                    <app-button texto="Desactivar" variante="peligro" tamano="pequeno" (presionado)="pedirEstado(s, false)" />
                  } @else {
                    <app-button texto="Reactivar" tamano="pequeno" (presionado)="pedirEstado(s, true)" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        [titulo]="activarSel() ? 'Reactivar Sede' : 'Desactivar Sede'"
        [mensaje]="activarSel() ? 'La sede volverá a estar activa y visible para los usuarios.' : 'Solo se desactivará si no tiene préstamos activos, reservas pendientes ni pedidos interciudad en curso. ¿Confirmas?'"
        [peligro]="!activarSel()"
        (confirmar)="ejecutarEstado()"
      />
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(195px, 1fr)); gap: 0.65rem; }`]
})
export class AdminSedesPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly editId = signal<number | null>(null);
  protected readonly errs = signal<Record<string, string>>({});
  protected readonly errorBloqueo = signal<string | null>(null);

  protected form: Partial<SedeDTO> = {
    nombre: '', ciudad: '', direccion: '', telefono: '', correo: '',
    horaApertura: '08:00', horaCierre: '20:00', aliada: false, enOperacion: true
  };

  protected readonly confirmOpen = signal(false);
  protected readonly sedeSel = signal<SedeDTO | null>(null);
  protected readonly activarSel = signal(false);

  ngOnInit(): void { this.cargar(); }
  private cargar(): void { this.catalogo.listarTodasSedes().subscribe((s) => this.sedes.set(s)); }

  protected editar(s: SedeDTO): void {
    this.editId.set(s.id);
    this.errs.set({});
    this.form = { ...s };
  }

  protected reset(): void {
    this.editId.set(null);
    this.errs.set({});
    this.form = { nombre: '', ciudad: '', direccion: '', telefono: '', correo: '', horaApertura: '08:00', horaCierre: '20:00', aliada: false, enOperacion: true };
  }

  protected guardar(): void {
    const e: Record<string, string> = {};
    if (!this.form.nombre?.trim()) e['nombre'] = 'El nombre es obligatorio.';
    if (!this.form.ciudad?.trim()) e['ciudad'] = 'La ciudad es obligatoria.';
    if (!this.form.direccion?.trim()) e['direccion'] = 'La dirección es obligatoria.';
    if (this.form.correo && !this.form.correo.includes('@')) e['correo'] = 'Correo inválido.';
    this.errs.set(e);
    if (Object.keys(e).length > 0) return;

    const id = this.editId();
    const obs = id
      ? this.catalogo.actualizarSede(id, this.form)
      : this.catalogo.crearSede({ ...this.form, enOperacion: true });
    obs.subscribe({
      next: () => { this.ui.mostrar(id ? 'Sede actualizada.' : 'Sede registrada.', 'exito'); this.reset(); this.cargar(); },
      error: (err: unknown) => this.ui.mostrar(extraerMensajeError(err), 'error'),
    });
  }

  protected pedirEstado(s: SedeDTO, activar: boolean): void {
    this.sedeSel.set(s);
    this.activarSel.set(activar);
    this.errorBloqueo.set(null);
    this.confirmOpen.set(true);
  }

  protected ejecutarEstado(): void {
    const s = this.sedeSel();
    if (!s) return;
    if (this.activarSel()) {
      this.catalogo.reactivarSede(s.id).subscribe({
        next: () => { this.ui.mostrar('Sede reactivada.', 'exito'); this.cargar(); },
        error: (err: unknown) => this.errorBloqueo.set(extraerMensajeError(err)),
      });
    } else {
      this.catalogo.desactivarSede(s.id).subscribe({
        next: () => { this.ui.mostrar('Sede desactivada (fuera de servicio).', 'info'); this.cargar(); },
        error: (err: unknown) => this.errorBloqueo.set(extraerMensajeError(err)),
      });
    }
  }
}
