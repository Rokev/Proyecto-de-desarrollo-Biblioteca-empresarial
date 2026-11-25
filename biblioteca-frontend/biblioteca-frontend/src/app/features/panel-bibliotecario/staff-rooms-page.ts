import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError } from 'rxjs';
import { SalaEstudioDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
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
    <app-panel-template titulo="Salas de Estudio de la Sede" subtitulo="Crea, edita y activa o desactiva las salas disponibles para reserva.">
      <app-card>
        <h3>{{ editandoId() ? 'Editar Sala #' + editandoId() : 'Registrar Nueva Sala' }}</h3>
        <form (ngSubmit)="guardar()" class="form-grid">
          <app-form-field etiqueta="ID" para="salaId" ayuda="Lo asigna el sistema al guardar.">
            <input id="salaId" class="campo" name="salaId" [value]="editandoId() ?? ''" placeholder="Automático" disabled />
          </app-form-field>

          <app-form-field etiqueta="Sede a la que pertenece" para="salaSede" [obligatorio]="true"
                          [ayuda]="sedeBloqueada() ? 'Tu usuario solo puede gestionar su sede.' : ''">
            <select id="salaSede" class="campo" name="salaSede" [(ngModel)]="sedeId" [disabled]="sedeBloqueada()">
              @for (s of sedes(); track s.id) { <option [ngValue]="s.id">{{ s.nombre }} ({{ s.ciudad }}) · ID {{ s.id }}</option> }
            </select>
          </app-form-field>

          <app-form-field etiqueta="Nombre de la sala" para="salaNombre" [obligatorio]="true">
            <input id="salaNombre" class="campo" name="salaNombre" [(ngModel)]="nombre" placeholder="Ej. Sala Cervantes" maxlength="100" required />
          </app-form-field>

          <app-form-field etiqueta="Capacidad (personas)" para="salaCap" [obligatorio]="true">
            <input id="salaCap" class="campo" type="number" min="1" name="salaCap" [(ngModel)]="capacidad" placeholder="Ej. 8" required />
          </app-form-field>

          <app-form-field etiqueta="Tarifa por hora" para="salaTarifa" [obligatorio]="true">
            <input id="salaTarifa" class="campo" type="number" min="0" step="500" name="salaTarifa" [(ngModel)]="tarifaPorHora" placeholder="Ej. 15000" required />
          </app-form-field>

          <app-form-field etiqueta="¿Sala activa?" para="salaActiva">
            <select id="salaActiva" class="campo" name="salaActiva" [(ngModel)]="activa">
              <option [ngValue]="true">Sí, activa</option>
              <option [ngValue]="false">No, inactiva</option>
            </select>
          </app-form-field>

          <app-form-field etiqueta="Descripción" para="salaDesc" class="ancho-completo">
            <textarea id="salaDesc" class="campo" name="salaDesc" rows="3" [(ngModel)]="descripcion"
                      placeholder="Ej. Sala con pizarra, proyector y aire acondicionado."></textarea>
          </app-form-field>

          <div class="acciones ancho-completo">
            <app-button tipo="submit" [texto]="guardando() ? 'Guardando…' : (editandoId() ? 'Guardar Cambios' : 'Crear Sala')" [deshabilitado]="guardando()" />
            @if (editandoId()) {
              <app-button texto="Cancelar edición" variante="contorno" (presionado)="limpiar()" />
            }
          </div>
        </form>
      </app-card>

      <h3 style="margin-top: 2rem;">Salas registradas</h3>
      <div class="tabla-responsive">
        <table class="tabla">
          <thead>
            <tr><th>ID</th><th>Sede</th><th>Nombre</th><th>Capacidad</th><th>Descripción</th><th>Tarifa / hora</th><th>Estado</th><th>Acciones</th></tr>
          </thead>
          <tbody>
            @for (s of salasVisibles(); track s.id) {
              <tr>
                <td>#{{ s.id }}</td>
                <td>{{ nombreSede(s.sedeId) }}</td>
                <td><strong>{{ s.nombre }}</strong></td>
                <td>{{ s.capacidad }}</td>
                <td>{{ s.descripcion || '—' }}</td>
                <td>{{ s.tarifaPorHora | currency }}</td>
                <td><app-badge [texto]="s.activa ? 'Activa' : 'Inactiva'" [estado]="s.activa ? 'ACTIVA' : 'FUERA_DE_SERVICIO'" /></td>
                <td class="celda-acciones">
                  <app-button texto="Editar" tamano="pequeno" variante="contorno" (presionado)="editar(s)" />
                  <app-button [texto]="s.activa ? 'Desactivar' : 'Activar'" tamano="pequeno" variante="secundario" (presionado)="alternarActiva(s)" />
                  <app-button texto="Eliminar" tamano="pequeno" variante="peligro" (presionado)="pedirEliminar(s)" />
                </td>
              </tr>
            } @empty {
              <tr><td colspan="8">Aún no hay salas registradas.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <app-confirm-dialog
        [(abierto)]="dialogoEliminar"
        titulo="Eliminar sala"
        [mensaje]="'¿Seguro que deseas eliminar la sala ' + (salaAEliminar()?.nombre ?? '') + '? Esta acción no se puede deshacer.'"
        textoConfirmar="Eliminar"
        [peligro]="true"
        (confirmar)="eliminar()"
      />
    </app-panel-template>
  `,
  styles: [`
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 0.75rem; }
    .ancho-completo { grid-column: 1 / -1; }
    .acciones { display: flex; gap: 0.6rem; }
    .celda-acciones { display: flex; gap: 0.4rem; flex-wrap: wrap; }
    textarea.campo { resize: vertical; }
  `]
})
export class StaffRoomsPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly catalogo = inject(CatalogoService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly salas = signal<SalaEstudioDTO[]>([]);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly guardando = signal(false);
  protected readonly editandoId = signal<number | null>(null);
  protected readonly salaAEliminar = signal<SalaEstudioDTO | null>(null);
  protected dialogoEliminar = false;

  /** Un bibliotecario con sede asignada solo gestiona las salas de su sede; el ADMIN gestiona todas. */
  private readonly sedePropia = computed(() => (this.auth.rol() === 'BIBLIOTECARIO' ? this.auth.perfil()?.sedeId ?? null : null));
  protected readonly sedeBloqueada = computed(() => this.sedePropia() !== null);
  protected readonly salasVisibles = computed(() => {
    const propia = this.sedePropia();
    return propia ? this.salas().filter((s) => s.sedeId === propia) : this.salas();
  });

  protected sedeId: number | null = null;
  protected nombre = '';
  protected capacidad: number | null = null;
  protected descripcion = '';
  protected tarifaPorHora: number | null = null;
  protected activa = true;

  ngOnInit(): void {
    this.cargar();
    this.catalogo.listarTodasSedes()
      .pipe(catchError(() => this.catalogo.listarSedesActivas()))
      .subscribe((s) => {
        this.sedeId = this.sedePropia() ?? this.auth.perfil()?.sedeId ?? s[0]?.id ?? null;
        this.sedes.set(s);
      });
  }

  private cargar(): void {
    this.ops.listarSalas().subscribe((s) => this.salas.set(s));
  }

  protected nombreSede(id: number): string {
    return this.sedes().find((s) => s.id === id)?.nombre ?? `Sede #${id}`;
  }

  protected limpiar(): void {
    this.editandoId.set(null);
    this.nombre = '';
    this.capacidad = null;
    this.descripcion = '';
    this.tarifaPorHora = null;
    this.activa = true;
    this.sedeId = this.sedePropia() ?? this.sedeId;
  }

  protected editar(s: SalaEstudioDTO): void {
    this.editandoId.set(s.id);
    this.sedeId = s.sedeId;
    this.nombre = s.nombre;
    this.capacidad = s.capacidad;
    this.descripcion = s.descripcion ?? '';
    this.tarifaPorHora = s.tarifaPorHora;
    this.activa = s.activa;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected guardar(): void {
    const capacidad = Number(this.capacidad);
    const tarifa = Number(this.tarifaPorHora);
    const sedeId = this.sedePropia() ?? this.sedeId;

    if (!sedeId || !this.nombre.trim() || !this.capacidad || this.tarifaPorHora === null) {
      this.ui.mostrar('Completa sede, nombre, capacidad y tarifa por hora.', 'advertencia');
      return;
    }
    if (!Number.isInteger(capacidad) || capacidad < 1) {
      this.ui.mostrar('La capacidad debe ser un número entero mayor a 0.', 'advertencia');
      return;
    }
    if (!Number.isFinite(tarifa) || tarifa < 0) {
      this.ui.mostrar('La tarifa por hora no puede ser negativa.', 'advertencia');
      return;
    }

    const dto: Partial<SalaEstudioDTO> = {
      nombre: this.nombre.trim(),
      capacidad,
      descripcion: this.descripcion.trim(),
      tarifaPorHora: tarifa,
      activa: this.activa,
      sedeId: Number(sedeId),
    };

    const id = this.editandoId();
    const peticion = id ? this.ops.actualizarSala(id, { ...dto, id }) : this.ops.crearSala(dto);

    this.guardando.set(true);
    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.ui.mostrar(id ? 'Sala actualizada correctamente.' : 'Sala creada correctamente.', 'exito');
        this.limpiar();
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.ui.mostrar(extraerMensajeError(e), 'error');
      },
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

  protected pedirEliminar(s: SalaEstudioDTO): void {
    this.salaAEliminar.set(s);
    this.dialogoEliminar = true;
  }

  protected eliminar(): void {
    const s = this.salaAEliminar();
    if (!s) return;
    this.ops.eliminarSala(s.id).subscribe({
      next: () => {
        this.ui.mostrar('Sala eliminada.', 'exito');
        if (this.editandoId() === s.id) this.limpiar();
        this.cargar();
      },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}