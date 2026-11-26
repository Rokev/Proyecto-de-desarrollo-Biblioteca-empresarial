import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MultaDTO, SedeDTO, SuscripcionDTO, UsuarioDTO } from '../../core/models/biblioteca.models';
import { OperacionesService } from '../../core/services/operaciones.service';
import { AdminService } from '../../core/services/admin.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-admin-users-page',
  imports: [FormsModule, RouterLink, CurrencyPipe, PanelTemplate, Card, Badge, Button, FormField, ConfirmDialog],
  template: `
    <app-panel-template titulo="Administración de Personal y Usuarios">
      <app-card>
        <h3>{{ editandoId() ? 'Editar Cuenta #' + editandoId() : 'Crear Cuenta de Personal (Bibliotecario / Repartidor)' }}</h3>
        <form (ngSubmit)="guardar()" novalidate>
          <div class="form-grid">
            <app-form-field etiqueta="Nombre" para="uNom" [obligatorio]="true" [error]="errs()['nombre'] || null">
              <input id="uNom" class="campo" [(ngModel)]="form.nombre" name="nom" />
            </app-form-field>
            <app-form-field etiqueta="Apellido" para="uApe" [obligatorio]="true" [error]="errs()['apellido'] || null">
              <input id="uApe" class="campo" [(ngModel)]="form.apellido" name="ape" />
            </app-form-field>
            <app-form-field etiqueta="Documento" para="uDoc" [obligatorio]="true" [error]="errs()['documento'] || null">
              <input id="uDoc" class="campo" [(ngModel)]="form.documento" name="doc" />
            </app-form-field>
            <app-form-field etiqueta="Nombre de usuario" para="uUsr" [obligatorio]="true" [error]="errs()['usuario'] || null">
              <input id="uUsr" class="campo" [(ngModel)]="form.nombreUsuario" name="usr" />
            </app-form-field>
            <app-form-field etiqueta="Correo" para="uCor" [obligatorio]="true" [error]="errs()['correo'] || null">
              <input id="uCor" type="email" class="campo" [(ngModel)]="form.correo" name="cor" />
            </app-form-field>
            @if (!editandoId()) {
              <app-form-field etiqueta="Contraseña (mín. 8)" para="uPas" [obligatorio]="true" [error]="errs()['pass'] || null">
                <input id="uPas" type="password" class="campo" [(ngModel)]="form.contrasena" name="pas" />
              </app-form-field>
            }
            <app-form-field etiqueta="Rol" para="uRol" [obligatorio]="true">
              <select id="uRol" class="campo" [(ngModel)]="form.rolId" name="rol">
                @for (r of rolesPersonal(); track r.id) { <option [ngValue]="r.id">{{ r.nombre }}</option> }
              </select>
            </app-form-field>
            @if (esRolBibliotecario(form.rolId)) {
              <app-form-field etiqueta="Sede asignada" para="uSed" [obligatorio]="true" [error]="errs()['sede'] || null">
                <select id="uSed" class="campo" [(ngModel)]="form.sedeId" name="sed">
                  <option [ngValue]="null" disabled>Seleccione sede...</option>
                  @for (s of sedes(); track s.id) { <option [ngValue]="s.id">{{ s.nombre }} ({{ s.ciudad }})</option> }
                </select>
              </app-form-field>
            }
          </div>
          <div style="display:flex; gap:0.6rem;">
            <app-button tipo="submit" [texto]="editandoId() ? 'Guardar Cambios' : 'Crear Cuenta'" />
            @if (editandoId()) { <app-button texto="Cancelar" variante="contorno" (presionado)="reset()" /> }
          </div>
        </form>
      </app-card>

      <!-- Pestañas Personal vs Usuarios -->
      <div style="display:flex; gap:0.5rem; margin: 1.5rem 0 1rem;">
        <app-button texto="Personal (Bibliotecarios y Repartidores)" [variante]="tab() === 'PERSONAL' ? 'primario' : 'contorno'" (presionado)="tab.set('PERSONAL')" />
        <app-button texto="Usuarios (Clientes)" [variante]="tab() === 'CLIENTES' ? 'primario' : 'contorno'" (presionado)="tab.set('CLIENTES')" />
      </div>

      <!-- Filtros combinables -->
      <div style="display:flex; gap:0.75rem; margin-bottom:1rem; flex-wrap:wrap;">
        <select class="campo" style="max-width:170px;" [(ngModel)]="filtroEstado">
          <option value="TODOS">Todos los estados</option>
          <option value="ACTIVO">Activos</option>
          <option value="INACTIVO">Inactivos / Suspendidos</option>
        </select>
        @if (tab() === 'PERSONAL') {
          <select class="campo" style="max-width:190px;" [(ngModel)]="filtroRol">
            <option [ngValue]="0">Todos los roles</option>
            @for (r of rolesPersonal(); track r.id) { <option [ngValue]="r.id">{{ r.nombre }}</option> }
          </select>
          <select class="campo" style="max-width:200px;" [(ngModel)]="filtroSede">
            <option [ngValue]="0">Todas las sedes</option>
            @for (s of sedes(); track s.id) { <option [ngValue]="s.id">{{ s.nombre }}</option> }
          </select>
        }
        @if (tab() === 'CLIENTES') {
          <select class="campo" style="max-width:200px;" [(ngModel)]="filtroSuscripcion">
            <option value="TODOS">Con o sin suscripción</option>
            <option value="CON">Con suscripción vigente</option>
            <option value="SIN">Sin suscripción</option>
          </select>
        }
        <app-button texto="Limpiar" variante="contorno" tamano="pequeno" (presionado)="limpiarFiltros()" />
      </div>

      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>Nombre</th><th>Usuario</th><th>Correo</th><th>Rol</th><th>{{ tab() === 'PERSONAL' ? 'Sede' : 'Suscripción' }}</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (u of usuariosFiltrados(); track u.id) {
              <tr>
                <td><strong>{{ u.nombre }} {{ u.apellido }}</strong></td>
                <td>{{ u.nombreUsuario }}</td>
                <td>{{ u.correo }}</td>
                <td>{{ nombreRol(u.rolId) }}</td>
                <td>{{ tab() === 'PERSONAL' ? nombreSede(u.sedeId) : (tieneSuscripcion(u.id) ? 'Vigente' : 'Sin suscripción') }}</td>
                <td>
                  <app-badge
                    [texto]="u.activo ? 'Activo' : (esPersonal(u.rolId) ? 'Desactivado' : 'Suspendido')"
                    [estado]="u.activo ? 'ACTIVA' : 'VENCIDO'"
                  />
                </td>
                <td style="display:flex; gap:0.35rem; flex-wrap:wrap;">
                  <app-button texto="Editar" variante="contorno" tamano="pequeno" (presionado)="editar(u)" />
                  <a [routerLink]="['/admin/auditoria']" [queryParams]="{ usuarioId: u.id }" style="color:var(--color-primario); font-weight:700; align-self:center; font-size:0.85rem;">Auditoría</a>
                  @if (!esPersonal(u.rolId)) {
                    <app-button texto="Multas" variante="secundario" tamano="pequeno" (presionado)="verMultas(u)" />
                  }
                  @if (u.activo) {
                    <app-button
                      [texto]="esPersonal(u.rolId) ? 'Desactivar' : 'Suspender'"
                      variante="peligro"
                      tamano="pequeno"
                      (presionado)="pedirCambioEstado(u, esPersonal(u.rolId) ? 'DESACTIVAR' : 'SUSPENDER')"
                    />
                  } @else {
                    <app-button texto="Reactivar" tamano="pequeno" (presionado)="pedirCambioEstado(u, 'REACTIVAR')" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        [titulo]="confirmTit()"
        [mensaje]="confirmMsg()"
        [peligro]="accionEstado() !== 'REACTIVAR'"
        [mostrarInput]="accionEstado() === 'SUSPENDER'"
        etiquetaInput="Motivo de suspensión (opcional)"
        (confirmar)="ejecutarCambioEstado()"
      />
          @if (multasModal(); as ml) {
        <div style="position:fixed; inset:0; background:rgba(15,23,42,.55); display:flex; align-items:center; justify-content:center; z-index:9500; padding:1rem;" (click)="multasModal.set(null)">
          <div style="background:#fff; padding:1.5rem; border-radius:var(--radio-lg); max-width:560px; width:100%;" (click)="$event.stopPropagation()">
            <h3>Multas y Recargos del Usuario</h3>
            <table class="tabla">
              <thead><tr><th>ID</th><th>Motivo</th><th>Monto</th><th>Estado</th></tr></thead>
              <tbody>
                @for (m of ml; track m.id) {
                  <tr><td>#{{ m.id }}</td><td>{{ m.motivo }}</td><td>{{ m.monto | currency:'COP':'symbol-narrow':'1.0-0' }}</td><td><app-badge [texto]="m.estado" [estado]="m.estado" /></td></tr>
                } @empty {
                  <tr><td colspan="4">Sin multas ni recargos.</td></tr>
                }
              </tbody>
            </table>
            <div style="margin-top:1rem; text-align:right;"><app-button texto="Cerrar" (presionado)="multasModal.set(null)" /></div>
          </div>
        </div>
      }
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(185px, 1fr)); gap: 0.65rem; }`]
})
export class AdminUsersPage implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly catalogo = inject(CatalogoService);
  private readonly ops = inject(OperacionesService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly usuarios = signal<UsuarioDTO[]>([]);
  protected readonly roles = signal<{ id: number; nombre: string }[]>([]);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly suscripciones = signal<SuscripcionDTO[]>([]);
  protected readonly multasModal = signal<MultaDTO[] | null>(null);
  protected filtroSuscripcion = 'TODOS';

  protected readonly tab = signal<'PERSONAL' | 'CLIENTES'>('PERSONAL');
  protected filtroEstado = 'TODOS';
  protected filtroRol = 0;
  protected filtroSede = 0;

  protected readonly editandoId = signal<number | null>(null);
  protected readonly errs = signal<Record<string, string>>({});
  protected form: Partial<UsuarioDTO> = { nombre: '', apellido: '', documento: '', nombreUsuario: '', correo: '', contrasena: '', rolId: 0, sedeId: undefined, activo: true };

  protected readonly confirmOpen = signal(false);
  protected readonly userSel = signal<UsuarioDTO | null>(null);
  protected readonly accionEstado = signal<'DESACTIVAR' | 'SUSPENDER' | 'REACTIVAR'>('SUSPENDER');
  protected readonly confirmTit = signal('');
  protected readonly confirmMsg = signal('');

  protected readonly rolesPersonal = computed(() =>
    this.roles().filter((r) => r.nombre.toUpperCase() === 'BIBLIOTECARIO' || r.nombre.toUpperCase() === 'REPARTIDOR')
  );

  protected readonly usuariosFiltrados = computed(() =>
    this.usuarios().filter((u) => {
      const esPers = this.esPersonal(u.rolId);
      if (this.tab() === 'PERSONAL' && !esPers) return false;
      if (this.tab() === 'CLIENTES' && esPers) return false;
      if (this.filtroEstado === 'ACTIVO' && !u.activo) return false;
      if (this.filtroEstado === 'INACTIVO' && u.activo) return false;
      if (this.tab() === 'PERSONAL' && this.filtroRol && u.rolId !== Number(this.filtroRol)) return false;
      if (this.tab() === 'PERSONAL' && this.filtroSede && u.sedeId !== Number(this.filtroSede)) return false;
      if (this.tab() === 'CLIENTES' && this.filtroSuscripcion === 'CON' && !this.tieneSuscripcion(u.id)) return false;
      if (this.tab() === 'CLIENTES' && this.filtroSuscripcion === 'SIN' && this.tieneSuscripcion(u.id)) return false;
      return true;
    })
  );

  ngOnInit(): void {
    this.cargar();
    this.admin.listarRoles().subscribe((r) => {
      this.roles.set(r);
      const bib = r.find((x) => x.nombre.toUpperCase() === 'BIBLIOTECARIO');
      if (bib) this.form.rolId = bib.id;
    });
    this.catalogo.listarTodasSedes().subscribe((s) => this.sedes.set(s));
    this.admin.listarSuscripciones().subscribe({ next: (su) => this.suscripciones.set(su), error: () => {} });
  }

  private cargar(): void { this.admin.listarUsuarios().subscribe((u) => this.usuarios.set(u)); }

  protected nombreRol(id: number): string { return this.roles().find((r) => r.id === id)?.nombre ?? `Rol #${id}`; }
  protected nombreSede(id?: number): string { return id ? (this.sedes().find((s) => s.id === id)?.nombre ?? `Sede #${id}`) : '-'; }
  protected esPersonal(rolId: number): boolean {
    const n = this.nombreRol(rolId).toUpperCase();
    return n === 'BIBLIOTECARIO' || n === 'REPARTIDOR';
  }
  protected esRolBibliotecario(rolId?: number): boolean {
    return !!rolId && this.nombreRol(rolId).toUpperCase() === 'BIBLIOTECARIO';
  }

  protected limpiarFiltros(): void { this.filtroEstado = 'TODOS'; this.filtroRol = 0; this.filtroSede = 0; this.filtroSuscripcion = 'TODOS'; }
  protected tieneSuscripcion(uid: number): boolean { return this.suscripciones().some((s) => s.usuarioId === uid && (s.estado === 'ACTIVA' || s.estado === 'CANCELADA')); }
  protected verMultas(u: UsuarioDTO): void { this.ops.multasUsuario(u.id).subscribe((m) => this.multasModal.set(m)); }

  protected editar(u: UsuarioDTO): void { this.editandoId.set(u.id); this.errs.set({}); this.form = { ...u, contrasena: '' }; }
  protected reset(): void {
    this.editandoId.set(null); this.errs.set({});
    this.form = { nombre: '', apellido: '', documento: '', nombreUsuario: '', correo: '', contrasena: '', rolId: this.rolesPersonal()[0]?.id ?? 0, sedeId: undefined, activo: true };
  }

  protected guardar(): void {
    const e: Record<string, string> = {};
    if (!this.form.nombre?.trim()) e['nombre'] = 'Obligatorio.';
    if (!this.form.apellido?.trim()) e['apellido'] = 'Obligatorio.';
    if (!this.form.documento?.trim()) e['documento'] = 'Obligatorio.';
    if (!this.form.nombreUsuario?.trim()) e['usuario'] = 'Obligatorio.';
    if (!this.form.correo?.includes('@')) e['correo'] = 'Correo inválido.';
    if (!this.editandoId() && (!this.form.contrasena || this.form.contrasena.length < 8)) e['pass'] = 'Mínimo 8 caracteres.';
    if (this.esRolBibliotecario(this.form.rolId) && !this.form.sedeId) e['sede'] = 'Sede obligatoria para bibliotecario.';
    this.errs.set(e);
    if (Object.keys(e).length > 0) return;

    const payload = { ...this.form, sedeId: this.esRolBibliotecario(this.form.rolId) ? this.form.sedeId : undefined };
    const id = this.editandoId();
    (id ? this.admin.actualizarUsuario(id, payload) : this.admin.crearUsuario({ ...payload, activo: true })).subscribe({
      next: () => { this.ui.mostrar(id ? 'Cuenta actualizada y auditada.' : 'Cuenta creada.', 'exito'); this.reset(); this.cargar(); },
      error: (err) => this.ui.mostrar(extraerMensajeError(err), 'error'),
    });
  }

  protected pedirCambioEstado(u: UsuarioDTO, acc: 'DESACTIVAR' | 'SUSPENDER' | 'REACTIVAR'): void {
    this.userSel.set(u); this.accionEstado.set(acc);
    this.confirmTit.set(`${acc} cuenta`);
    this.confirmMsg.set(`¿Confirmas ${acc.toLowerCase()} la cuenta de ${u.nombre} ${u.apellido}?`);
    this.confirmOpen.set(true);
  }

  protected ejecutarCambioEstado(): void {
    const u = this.userSel(); if (!u) return;
    const acc = this.accionEstado();
    const obs = acc === 'DESACTIVAR' ? this.admin.desactivarUsuario(u.id)
      : acc === 'SUSPENDER' ? this.admin.suspenderUsuario(u.id)
      : this.admin.reactivarUsuario(u.id);
    obs.subscribe({
      next: () => { this.ui.mostrar(`Acción ${acc} ejecutada y registrada en auditoría.`, 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
