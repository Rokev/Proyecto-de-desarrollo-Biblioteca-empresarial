import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  AuditoriaDTO,
  PagoDTO,
  PlanSuscripcionDTO,
  PlazoSistemaDTO,
  PoliticaMultaDTO,
  PoliticaSancionDTO,
  PreguntaFrecuenteDTO,
  ReporteDTO,
  SedeDTO,
  TarifaEnvioDTO,
  UsuarioDTO,
} from '../../core/models/biblioteca.models';
import { AdminService } from '../../core/services/admin.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { ConfirmDialog } from '../../shared/ui/molecules/confirm-dialog/confirm-dialog';
import { StatCard } from '../../shared/ui/molecules/stat-card/stat-card';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-admin-plans-page',
  imports: [FormsModule, CurrencyPipe, PanelTemplate, Card, Badge, Button, ConfirmDialog],
  template: `
    <app-panel-template titulo="Planes de Suscripción">
      <app-card>
        <h3>{{ editId() ? 'Editar Plan #' + editId() : 'Nuevo Plan' }}</h3>
        <form (ngSubmit)="guardar()" class="form-grid">
          <input class="campo" placeholder="Nombre *" [(ngModel)]="form.nombre" name="nom" required />
          <input class="campo" placeholder="Descripción *" [(ngModel)]="form.descripcion" name="des" required />
          <input class="campo" placeholder="Beneficios *" [(ngModel)]="form.beneficios" name="ben" required />
          <input class="campo" type="number" placeholder="Precio COP *" [(ngModel)]="form.precio" name="pre" required />
          <input class="campo" type="number" placeholder="Duración meses *" [(ngModel)]="form.duracionMeses" name="dur" required />
          <input class="campo" type="number" placeholder="% Descuento salas" [(ngModel)]="form.descuentoSalas" name="dsc" />
          <app-button tipo="submit" [texto]="editId() ? 'Actualizar Plan' : 'Crear Plan'" />
          @if (editId()) { <app-button texto="Cancelar" variante="contorno" (presionado)="reset()" /> }
        </form>
      </app-card>
      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>Plan</th><th>Precio</th><th>Meses</th><th>Desc. Salas</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (p of planes(); track p.id) {
              <tr>
                <td><strong>{{ p.nombre }}</strong></td>
                <td>{{ p.precio | currency:'COP':'symbol-narrow':'1.0-0' }}</td>
                <td>{{ p.duracionMeses }}</td>
                <td>{{ p.descuentoSalas }}%</td>
                <td><app-badge [texto]="p.activo ? 'Activo' : 'Inactivo'" [estado]="p.activo ? 'ACTIVA' : 'VENCIDO'" /></td>
                <td style="display:flex; gap:0.4rem;">
                  <app-button texto="Editar" variante="contorno" tamano="pequeno" (presionado)="editar(p)" />
                  @if (p.activo) { <app-button texto="Desactivar" variante="peligro" tamano="pequeno" (presionado)="pedirEstado(p, false)" /> }
                  @else { <app-button texto="Reactivar" tamano="pequeno" (presionado)="pedirEstado(p, true)" /> }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        [titulo]="activarSel() ? 'Reactivar Plan' : 'Desactivar Plan'"
        [mensaje]="activarSel() ? 'El plan volverá a estar disponible para nuevas compras.' : 'Se impedirán nuevas compras de este plan. Los suscriptores actuales conservarán sus beneficios hasta finalizar su período.'"
        [peligro]="!activarSel()"
        (confirmar)="ejecutarEstado()"
      />
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 0.65rem; }`]
})
export class AdminPlansPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly admin = inject(AdminService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly planes = signal<PlanSuscripcionDTO[]>([]);
  protected readonly editId = signal<number | null>(null);
  protected form: Partial<PlanSuscripcionDTO> = { nombre: '', descripcion: '', beneficios: '', precio: 25000, duracionMeses: 1, descuentoSalas: 20, activo: true };

  protected readonly confirmOpen = signal(false);
  protected readonly planSel = signal<PlanSuscripcionDTO | null>(null);
  protected readonly activarSel = signal(false);

  ngOnInit(): void { this.cargar(); }
  private cargar(): void { this.ops.listarPlanes().subscribe((p) => this.planes.set(p)); }
  protected editar(p: PlanSuscripcionDTO): void { this.editId.set(p.id); this.form = { ...p }; }
  protected reset(): void { this.editId.set(null); this.form = { nombre: '', descripcion: '', beneficios: '', precio: 25000, duracionMeses: 1, descuentoSalas: 20, activo: true }; }

  protected guardar(): void {
    const id = this.editId();
    (id ? this.admin.actualizarPlan(id, this.form) : this.admin.crearPlan(this.form)).subscribe({
      next: () => { this.ui.mostrar(id ? 'Plan actualizado.' : 'Plan creado.', 'exito'); this.reset(); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected pedirEstado(p: PlanSuscripcionDTO, act: boolean): void {
    this.planSel.set(p); this.activarSel.set(act); this.confirmOpen.set(true);
  }
  protected ejecutarEstado(): void {
    const p = this.planSel(); if (!p) return;
    (this.activarSel() ? this.admin.reactivarPlan(p.id) : this.admin.desactivarPlan(p.id)).subscribe(() => {
      this.ui.mostrar('Estado del plan actualizado.', 'exito'); this.cargar();
    });
  }
}

@Component({
  selector: 'app-admin-shipping-page',
  imports: [FormsModule, RouterLink, CurrencyPipe, PanelTemplate, Card, Button],
  template: `
    <app-panel-template titulo="Tarifas de Envío Interciudad" subtitulo="Aplica por igual en ambos sentidos entre parejas de ciudades.">
      <app-card>
        <h3>{{ editId() ? 'Editar Tarifa #' + editId() : 'Cargar Nueva Tarifa' }}</h3>
        <form (ngSubmit)="guardar()" class="form-grid">
          <select class="campo" [(ngModel)]="ciudadA" name="ca" required>
            <option value="" disabled>Ciudad A...</option>
            @for (c of ciudades(); track c) { <option [value]="c">{{ c }}</option> }
          </select>
          <select class="campo" [(ngModel)]="ciudadB" name="cb" required>
            <option value="" disabled>Ciudad B...</option>
            @for (c of ciudades(); track c) { <option [value]="c">{{ c }}</option> }
          </select>
          <input class="campo" type="number" min="1" placeholder="Tarifa COP (> 0)" [(ngModel)]="tarifa" name="tar" required />
          <app-button tipo="submit" [texto]="editId() ? 'Actualizar' : 'Guardar Tarifa'" />
          @if (editId()) { <app-button texto="Cancelar" variante="contorno" (presionado)="reset()" /> }
        </form>
      </app-card>
      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>Ciudad A</th><th>Ciudad B</th><th>Tarifa</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (t of tarifas(); track t.id) {
              <tr>
                <td>{{ t.ciudadA }}</td><td>{{ t.ciudadB }}</td><td>{{ t.tarifa | currency:'COP':'symbol-narrow':'1.0-0' }}</td>
                <td style="display:flex; gap:0.5rem;">
                  <app-button texto="Editar" variante="contorno" tamano="pequeno" (presionado)="editar(t)" />
                  <a routerLink="/admin/auditoria" [queryParams]="{ modulo: 'TARIFAS_ENVIO' }" style="color:var(--color-primario); font-weight:700; align-self:center; font-size:0.85rem;">Ver Auditoría</a>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 0.65rem; }`]
})
export class AdminShippingPage implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly catalogo = inject(CatalogoService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly tarifas = signal<TarifaEnvioDTO[]>([]);
  protected readonly ciudades = signal<string[]>([]);
  protected readonly editId = signal<number | undefined>(undefined);
  protected ciudadA = ''; protected ciudadB = ''; protected tarifa = 12000;

  ngOnInit(): void {
    this.cargar();
    this.catalogo.listarTodasSedes().subscribe((s) => {
      this.ciudades.set([...new Set(s.map((x) => x.ciudad))]);
    });
  }
  private cargar(): void { this.admin.listarTarifas().subscribe((t) => this.tarifas.set(t)); }
  protected editar(t: TarifaEnvioDTO): void { this.editId.set(t.id); this.ciudadA = t.ciudadA; this.ciudadB = t.ciudadB; this.tarifa = t.tarifa; }
  protected reset(): void { this.editId.set(undefined); this.ciudadA = ''; this.ciudadB = ''; this.tarifa = 12000; }

  protected guardar(): void {
    if (!this.ciudadA || !this.ciudadB || this.ciudadA === this.ciudadB) {
      this.ui.mostrar('Selecciona dos ciudades distintas.', 'error'); return;
    }
    if (this.tarifa <= 0) { this.ui.mostrar('La tarifa debe ser mayor a 0.', 'error'); return; }
    this.admin.guardarTarifa({ id: this.editId(), ciudadA: this.ciudadA, ciudadB: this.ciudadB, tarifa: this.tarifa }).subscribe({
      next: () => { this.ui.mostrar('Tarifa guardada y auditada.', 'exito'); this.reset(); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}

@Component({
  selector: 'app-admin-reports-page',
  imports: [FormsModule, PanelTemplate, Card, Button, StatCard, Alert],
  template: `
    <app-panel-template titulo="Generación de Reportes (Vista Previa, PDF y CSV)">
      <app-card>
        <div class="form-grid">
          <select class="campo" [(ngModel)]="tipo">
            @for (t of tipos(); track t) { <option [value]="t">{{ etiquetaTipo(t) }}</option> }
          </select>
          <select class="campo" [(ngModel)]="sedeId">
            <option [ngValue]="0">Todas las sedes</option>
            @for (s of sedes(); track s.id) { <option [ngValue]="s.id">{{ s.nombre }}</option> }
          </select>
          <input type="date" class="campo" [(ngModel)]="desde" />
          <input type="date" class="campo" [(ngModel)]="hasta" />
          <app-button texto="Generar Vista Previa" (presionado)="previsualizar()" />
          <app-button texto="Exportar PDF" variante="secundario" (presionado)="descargar('PDF')" />
          <app-button texto="Exportar CSV" variante="contorno" (presionado)="descargar('CSV')" />
        </div>
      </app-card>

      @if (reporte(); as r) {
        <h3 style="margin-top:1.5rem;">{{ r.titulo }} ({{ r.totalRegistros }} registros)</h3>
        @if (r.totalRegistros === 0) {
          <app-alert titulo="Sin datos" mensaje="No se encontraron registros para los filtros seleccionados." tipo="info" />
        } @else {
          <div class="tabla-responsive">
            <table class="tabla">
              <thead><tr>@for (col of r.columnas; track col) { <th>{{ col }}</th> }</tr></thead>
              <tbody>
                @for (fila of r.filas; track $index) {
                  <tr>@for (val of fila; track $index) { <td>{{ val }}</td> }</tr>
                }
              </tbody>
            </table>
          </div>
        }
      }
    </app-panel-template>
  `,
  styles: [`.form-grid { display: flex; gap: 0.75rem; flex-wrap: wrap; }`]
})
export class AdminReportsPage implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly catalogo = inject(CatalogoService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly tipos = signal<string[]>([]);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly reporte = signal<ReporteDTO | null>(null);
  protected tipo = 'PRESTAMOS'; protected sedeId = 0; protected desde = ''; protected hasta = '';

  ngOnInit(): void {
    this.admin.tiposReporte().subscribe((t) => { this.tipos.set(t); if (t[0]) this.tipo = t[0]; });
    this.catalogo.listarTodasSedes().subscribe((s) => this.sedes.set(s));
  }

  protected etiquetaTipo(t: string): string {
    const map: Record<string, string> = {
      USO: 'Uso de Plataforma', PRESTAMOS: 'Préstamos', RESERVAS: 'Reservas',
      EVENTOS: 'Eventos', SUSCRIPCIONES: 'Suscripciones', PEDIDOS_INTERCIUDAD: 'Pedidos Interciudad'
    };
    return map[t] ?? t;
  }

  protected previsualizar(): void {
    this.admin.generarReporteJson(this.tipo, this.desde || undefined, this.hasta || undefined, this.sedeId || undefined).subscribe({
      next: (r) => this.reporte.set(r),
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }

  protected descargar(formato: 'PDF' | 'CSV'): void {
    this.admin.exportarReporte(this.tipo, formato, this.desde || undefined, this.hasta || undefined, this.sedeId || undefined).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = `reporte-${this.tipo.toLowerCase()}.${formato.toLowerCase()}`; a.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}

@Component({
  selector: 'app-admin-payments-page',
  imports: [FormsModule, CurrencyPipe, DatePipe, PanelTemplate, Badge, Button],
  template: `
    <app-panel-template titulo="Historial Global de Pagos y Reembolsos">
      <div style="display: flex; gap: 0.75rem; margin-bottom: 1rem; flex-wrap: wrap;">
        <select class="campo" style="max-width: 190px;" [(ngModel)]="concepto">
          <option value="">Todos los conceptos</option>
          <option value="SUSCRIPCION">Suscripción</option>
          <option value="ENVIO_INTERCIUDAD">Envío Interciudad</option>
          <option value="MULTA">Multa</option>
          <option value="RECARGO">Recargo</option>
          <option value="RESERVA_SALA">Reserva Sala</option>
        </select>
        <select class="campo" style="max-width: 170px;" [(ngModel)]="estado">
          <option value="">Todos los estados</option>
          <option value="APROBADO">Aprobado</option>
          <option value="RECHAZADO">Rechazado</option>
          <option value="REEMBOLSADO">Reembolsado</option>
        </select>
        <input type="date" class="campo" style="max-width: 165px;" [(ngModel)]="desde" />
        <input type="date" class="campo" style="max-width: 165px;" [(ngModel)]="hasta" />
        <app-button texto="Filtrar" (presionado)="buscar()" />
        <app-button texto="Limpiar" variante="contorno" (presionado)="limpiar()" />
      </div>
      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>Referencia</th><th>Usuario</th><th>Concepto</th><th>Monto</th><th>Estado</th><th>Fecha</th></tr></thead>
          <tbody>
            @for (p of pagos(); track p.id) {
              <tr>
                <td>{{ p.referencia }}</td>
                <td>{{ nombreUsuario(p.usuarioId) }}</td>
                <td>{{ p.concepto }}</td>
                <td>{{ p.monto | currency:'COP':'symbol-narrow':'1.0-0' }}</td>
                <td><app-badge [texto]="p.estado" [estado]="p.estado" /></td>
                <td>{{ p.fechaPago | date:'d MMM y, h:mm a' }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `
})
export class AdminPaymentsPage implements OnInit {
  private readonly admin = inject(AdminService);
  protected readonly pagos = signal<PagoDTO[]>([]);
  protected readonly usuarios = signal<UsuarioDTO[]>([]);
  protected concepto = ''; protected estado = ''; protected desde = ''; protected hasta = '';

  ngOnInit(): void {
    this.admin.listarUsuarios().subscribe((u) => this.usuarios.set(u));
    this.buscar();
  }
  protected buscar(): void {
    this.admin.filtrarPagos(this.concepto, this.estado, this.desde || undefined, this.hasta || undefined).subscribe((p) => this.pagos.set(p));
  }
  protected limpiar(): void { this.concepto = ''; this.estado = ''; this.desde = ''; this.hasta = ''; this.buscar(); }
  protected nombreUsuario(id: number): string {
    const u = this.usuarios().find((x) => x.id === id);
    return u ? `${u.nombre} ${u.apellido}` : `Usuario #${id}`;
  }
}

@Component({
  selector: 'app-admin-audit-page',
  imports: [FormsModule, DatePipe, PanelTemplate, Button],
  template: `
    <app-panel-template titulo="Historial de Auditoría del Sistema">
      <div style="display:flex; gap:0.65rem; margin-bottom:1rem; flex-wrap:wrap;">
        <input type="number" class="campo" style="max-width:140px;" placeholder="Usuario ID" [(ngModel)]="fUsuarioId" />
        <select class="campo" style="max-width:170px;" [(ngModel)]="fModulo">
          <option value="">Todos los módulos</option>
          <option value="USUARIOS">USUARIOS</option><option value="PERSONAL">PERSONAL</option>
          <option value="SEDES">SEDES</option><option value="PLANES">PLANES</option>
          <option value="POLITICAS">POLITICAS</option><option value="TARIFAS_ENVIO">TARIFAS_ENVIO</option>
          <option value="REPORTES">REPORTES</option><option value="CHATBOT">CHATBOT</option>
        </select>
        <select class="campo" style="max-width:170px;" [(ngModel)]="fAccion">
          <option value="">Todas las acciones</option>
          <option value="CREAR">CREAR</option><option value="MODIFICAR">MODIFICAR</option>
          <option value="DESACTIVAR">DESACTIVAR</option><option value="REACTIVAR">REACTIVAR</option>
          <option value="SUSPENDER">SUSPENDER</option><option value="CONFIGURAR">CONFIGURAR</option>
        </select>
        <input type="date" class="campo" style="max-width:155px;" [(ngModel)]="fDesde" />
        <input type="date" class="campo" style="max-width:155px;" [(ngModel)]="fHasta" />
        <app-button texto="Filtrar" (presionado)="buscar(0)" />
        <app-button texto="Limpiar" variante="contorno" (presionado)="limpiar()" />
      </div>

      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>Fecha/Hora</th><th>Módulo</th><th>Acción</th><th>Descripción</th><th>Usuario</th></tr></thead>
          <tbody>
            @for (a of logs(); track a.id) {
              <tr>
                <td>{{ a.fecha | date:'d MMM y, h:mm:ss a' }}</td>
                <td>{{ a.modulo }}</td><td>{{ a.accion }}</td><td>{{ a.descripcion }}</td><td>{{ a.usuarioNombre || a.usuarioId || 'Sistema' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="5">Sin registros de auditoría para los filtros aplicados.</td></tr>
            }
          </tbody>
        </table>
      </div>
      <div style="display:flex; justify-content:space-between; margin-top:1rem;">
        <app-button texto="← Anterior" variante="contorno" tamano="pequeno" [deshabilitado]="pagina() === 0" (presionado)="buscar(pagina() - 1)" />
        <span>Página {{ pagina() + 1 }}</span>
        <app-button texto="Siguiente →" variante="contorno" tamano="pequeno" [deshabilitado]="logs().length < 25" (presionado)="buscar(pagina() + 1)" />
      </div>
    </app-panel-template>
  `
})
export class AdminAuditPage implements OnInit {
  private readonly admin = inject(AdminService);
  readonly usuarioId = input<string>();
  readonly modulo = input<string>();

  protected readonly logs = signal<AuditoriaDTO[]>([]);
  protected readonly pagina = signal(0);
  protected fUsuarioId: number | null = null;
  protected fModulo = ''; protected fAccion = ''; protected fDesde = ''; protected fHasta = '';

  ngOnInit(): void {
    if (this.usuarioId()) this.fUsuarioId = Number(this.usuarioId());
    if (this.modulo()) this.fModulo = this.modulo()!;
    this.buscar(0);
  }

  protected buscar(pag: number): void {
    this.pagina.set(pag);
    this.admin.listarAuditoria({
      usuarioId: this.fUsuarioId, modulo: this.fModulo || undefined,
      accion: this.fAccion || undefined, desde: this.fDesde || undefined,
      hasta: this.fHasta || undefined, pagina: pag, tamanio: 25
    }).subscribe((l) => this.logs.set(l));
  }

  protected limpiar(): void {
    this.fUsuarioId = null; this.fModulo = ''; this.fAccion = ''; this.fDesde = ''; this.fHasta = '';
    this.buscar(0);
  }
}

@Component({
  selector: 'app-admin-faq-page',
  imports: [FormsModule, PanelTemplate, Card, Button, ConfirmDialog, Alert],
  template: `
    <app-panel-template titulo="Base de Conocimiento del Chatbot (FAQ)">
      <div class="grid-cards" style="margin-bottom:1.5rem;">
        <app-card>
          <h3>{{ editId() ? 'Editar Respuesta #' + editId() : 'Agregar Respuesta / Flujo' }}</h3>
          <form (ngSubmit)="guardar()" style="display:flex; flex-direction:column; gap:0.6rem;">
            <input class="campo" placeholder="Pregunta frecuente *" [(ngModel)]="pregunta" name="pre" required />
            <textarea class="campo" rows="2" placeholder="Respuesta *" [(ngModel)]="respuesta" name="res" required></textarea>
            <input class="campo" placeholder="Categoría" [(ngModel)]="categoria" name="cat" />
            <div style="display:flex; gap:0.5rem;">
              <app-button tipo="submit" [texto]="editId() ? 'Actualizar' : 'Guardar'" />
              @if (editId()) { <app-button texto="Cancelar" variante="contorno" (presionado)="reset()" /> }
            </div>
          </form>
        </app-card>

        <app-card>
          <h3>Probar en el Chatbot</h3>
          <div style="display:flex; gap:0.5rem; margin-bottom:0.75rem;">
            <input class="campo" placeholder="Escribe una pregunta de prueba..." [(ngModel)]="pruebaMsg" />
            <app-button texto="Probar" (presionado)="probarBot()" />
          </div>
          @if (respuestaPrueba()) {
            <app-alert titulo="Respuesta del Chatbot" [mensaje]="respuestaPrueba()!" tipo="info" />
          }
        </app-card>
      </div>

      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>Categoría</th><th>Pregunta</th><th>Respuesta</th><th>Acciones</th></tr></thead>
          <tbody>
            @for (f of faqs(); track f.id) {
              <tr>
                <td>{{ f.categoria }}</td><td>{{ f.pregunta }}</td><td>{{ f.respuesta }}</td>
                <td style="display:flex; gap:0.4rem;">
                  <app-button texto="Editar" variante="contorno" tamano="pequeno" (presionado)="editar(f)" />
                  <app-button texto="Eliminar" variante="peligro" tamano="pequeno" (presionado)="pedirEliminar(f.id!)" />
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        titulo="Eliminar Pregunta Frecuente"
        mensaje="¿Confirmas eliminar esta respuesta de la base de conocimiento del chatbot?"
        [peligro]="true"
        (confirmar)="ejecutarEliminar()"
      />
    </app-panel-template>
  `
})
export class AdminFaqPage implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly faqs = signal<PreguntaFrecuenteDTO[]>([]);
  protected readonly editId = signal<number | null>(null);
  protected pregunta = ''; protected respuesta = ''; protected categoria = 'GENERAL';

  protected pruebaMsg = '';
  protected readonly respuestaPrueba = signal<string | null>(null);
  protected readonly confirmOpen = signal(false);
  protected readonly idSel = signal<number | null>(null);

  ngOnInit(): void { this.cargar(); }
  private cargar(): void { this.admin.listarFaqs().subscribe((f) => this.faqs.set(f)); }
  protected editar(f: PreguntaFrecuenteDTO): void { this.editId.set(f.id ?? null); this.pregunta = f.pregunta; this.respuesta = f.respuesta; this.categoria = f.categoria; }
  protected reset(): void { this.editId.set(null); this.pregunta = ''; this.respuesta = ''; this.categoria = 'GENERAL'; }

  protected guardar(): void {
    const dto: PreguntaFrecuenteDTO = { pregunta: this.pregunta, respuesta: this.respuesta, categoria: this.categoria, activa: true };
    const id = this.editId();
    (id ? this.admin.actualizarFaq(id, dto) : this.admin.crearFaq(dto)).subscribe(() => {
      this.ui.mostrar(id ? 'FAQ actualizada.' : 'FAQ agregada.', 'exito');
      this.reset(); this.cargar();
    });
  }

  protected probarBot(): void {
    if (!this.pruebaMsg.trim()) return;
    this.admin.enviarMensajeChatbot(this.pruebaMsg.trim()).subscribe((r) => this.respuestaPrueba.set(r.respuesta));
  }

  protected pedirEliminar(id: number): void { this.idSel.set(id); this.confirmOpen.set(true); }
  protected ejecutarEliminar(): void {
    const id = this.idSel(); if (!id) return;
    this.admin.eliminarFaq(id).subscribe(() => { this.ui.mostrar('FAQ eliminada.', 'info'); this.cargar(); });
  }
}

@Component({
  selector: 'app-admin-policies-page',
  imports: [FormsModule, PanelTemplate, Card, Button, ConfirmDialog],
  template: `
    <app-panel-template titulo="Políticas de Sanción, Multas y Plazos">
      <div class="grid-cards">
        <!-- Prueba 126: Políticas de Sanción -->
        <app-card>
          <h3>Políticas de Sanción</h3>
          @for (s of sanciones(); track s.infraccion) {
            <div class="item-pol">
              <strong>{{ s.descripcion || s.infraccion }}</strong>
              <select class="campo" [(ngModel)]="s.tipoSancion">
                <option value="BLOQUEO_PRESTAMOS">Bloqueo temporal préstamos</option>
                <option value="SUSPENSION_CUENTA">Suspensión de cuenta</option>
                <option value="ADVERTENCIA">Solo advertencia</option>
              </select>
              <input type="number" min="1" class="campo" placeholder="Días de sanción" [(ngModel)]="s.duracionDias" />
              <app-button texto="Guardar Sanción" tamano="pequeno" (presionado)="pedirGuardar('SANCION', s)" />
            </div>
          }
        </app-card>

        <app-card>
          <h3>Políticas de Multas y Recargos (COP)</h3>
          @for (m of multas(); track m.concepto) {
            <div class="item-pol">
              <strong>{{ m.descripcion || m.concepto }}</strong>
              <input type="number" min="1" class="campo" [(ngModel)]="m.monto" />
              <app-button texto="Guardar Monto" tamano="pequeno" (presionado)="pedirGuardar('MULTA', m)" />
            </div>
          }
        </app-card>

        <app-card>
          <h3>Plazos del Sistema</h3>
          @for (p of plazos(); track p.clave) {
            <div class="item-pol">
              <strong>{{ p.descripcion || p.clave }} ({{ p.unidad || 'DIAS' }})</strong>
              <input type="number" min="1" class="campo" [(ngModel)]="p.valor" />
              <app-button texto="Guardar Plazo" tamano="pequeno" (presionado)="pedirGuardar('PLAZO', p)" />
            </div>
          }
        </app-card>
      </div>

      <app-confirm-dialog
        [(abierto)]="confirmOpen"
        titulo="Confirmar Cambio de Política"
        mensaje="¿Confirmas guardar esta configuración del sistema? Se aplicará de inmediato y quedará registrada en auditoría."
        (confirmar)="ejecutarGuardar()"
      />
    </app-panel-template>
  `,
  styles: [`.item-pol { margin-bottom: 1rem; padding-bottom: 0.75rem; border-bottom: 1px solid var(--color-borde); display:flex; flex-direction:column; gap:0.4rem; }`]
})
export class AdminPoliciesPage implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly sanciones = signal<PoliticaSancionDTO[]>([]);
  protected readonly multas = signal<PoliticaMultaDTO[]>([]);
  protected readonly plazos = signal<PlazoSistemaDTO[]>([]);

  protected readonly confirmOpen = signal(false);
  private tipoPendiente: 'SANCION' | 'MULTA' | 'PLAZO' = 'MULTA';
  private itemPendiente: any = null;

  ngOnInit(): void {
    this.admin.obtenerPoliticasSancion().subscribe((s) => this.sanciones.set(s));
    this.admin.obtenerPoliticasMulta().subscribe((m) => this.multas.set(m));
    this.admin.obtenerPlazos().subscribe((p) => this.plazos.set(p));
  }

  protected pedirGuardar(tipo: 'SANCION' | 'MULTA' | 'PLAZO', item: any): void {
    if (tipo === 'MULTA' && item.monto <= 0) { this.ui.mostrar('El monto debe ser mayor a 0.', 'error'); return; }
    if (tipo === 'PLAZO' && item.valor <= 0) { this.ui.mostrar('El plazo debe ser mayor a 0.', 'error'); return; }
    this.tipoPendiente = tipo; this.itemPendiente = item; this.confirmOpen.set(true);
  }

  protected ejecutarGuardar(): void {
    const item = this.itemPendiente;
    if (!item) return;
    if (this.tipoPendiente === 'SANCION') {
      this.admin.actualizarPoliticaSancion(item.infraccion, item).subscribe(() => this.ui.mostrar('Política de sanción guardada.', 'exito'));
    } else if (this.tipoPendiente === 'MULTA') {
      this.admin.actualizarPoliticaMulta(item.concepto, item).subscribe(() => this.ui.mostrar('Política de multa guardada.', 'exito'));
    } else {
      this.admin.actualizarPlazo(item.clave, item).subscribe(() => this.ui.mostrar('Plazo guardado.', 'exito'));
    }
  }
}
