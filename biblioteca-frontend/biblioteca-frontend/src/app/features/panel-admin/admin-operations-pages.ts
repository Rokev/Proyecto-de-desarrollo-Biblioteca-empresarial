import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditoriaDTO, PagoDTO, PlanSuscripcionDTO, PreguntaFrecuenteDTO, TarifaEnvioDTO } from '../../core/models/biblioteca.models';
import { AdminService } from '../../core/services/admin.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { OperacionesService } from '../../core/services/operaciones.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

@Component({
  selector: 'app-admin-plans-page',
  imports: [FormsModule, CurrencyPipe, PanelTemplate, Card, Badge, Button],
  template: `
    <app-panel-template titulo="Planes de Suscripción">
      <app-card>
        <h3>Nuevo Plan</h3>
        <form (ngSubmit)="crear()" class="form-grid">
          <input class="campo" placeholder="Nombre *" [(ngModel)]="nuevo.nombre" name="nom" required />
          <input class="campo" placeholder="Descripción *" [(ngModel)]="nuevo.descripcion" name="des" required />
          <input class="campo" placeholder="Beneficios *" [(ngModel)]="nuevo.beneficios" name="ben" required />
          <input class="campo" type="number" placeholder="Precio COP *" [(ngModel)]="nuevo.precio" name="pre" required />
          <input class="campo" type="number" placeholder="Duración meses *" [(ngModel)]="nuevo.duracionMeses" name="dur" required />
          <input class="campo" type="number" placeholder="% Descuento salas" [(ngModel)]="nuevo.descuentoSalas" name="dsc" />
          <app-button tipo="submit" texto="Crear Plan" />
        </form>
      </app-card>
      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>Plan</th><th>Precio</th><th>Meses</th><th>Desc. Salas</th><th>Estado</th><th>Acción</th></tr></thead>
          <tbody>
            @for (p of planes(); track p.id) {
              <tr>
                <td>{{ p.nombre }}</td><td>{{ p.precio | currency:'COP':'symbol-narrow':'1.0-0' }}</td><td>{{ p.duracionMeses }}</td><td>{{ p.descuentoSalas }}%</td>
                <td><app-badge [texto]="p.activo ? 'Activo' : 'Inactivo'" [estado]="p.activo ? 'ACTIVA' : 'VENCIDO'" /></td>
                <td>
                  @if (p.activo) { <app-button texto="Desactivar" variante="peligro" tamano="pequeno" (presionado)="desactivar(p.id)" /> }
                  @else { <app-button texto="Reactivar" tamano="pequeno" (presionado)="reactivar(p.id)" /> }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 0.65rem; }`]
})
export class AdminPlansPage implements OnInit {
  private readonly ops = inject(OperacionesService);
  private readonly admin = inject(AdminService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly planes = signal<PlanSuscripcionDTO[]>([]);
  protected nuevo: Partial<PlanSuscripcionDTO> = { nombre: '', descripcion: '', beneficios: '', precio: 25000, duracionMeses: 1, descuentoSalas: 20, activo: true };

  ngOnInit(): void { this.cargar(); }
  private cargar(): void { this.ops.listarPlanes().subscribe((p) => this.planes.set(p)); }

  protected crear(): void {
    this.admin.crearPlan(this.nuevo).subscribe({
      next: () => { this.ui.mostrar('Plan creado.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
  protected desactivar(id: number): void { this.admin.desactivarPlan(id).subscribe(() => this.cargar()); }
  protected reactivar(id: number): void { this.admin.reactivarPlan(id).subscribe(() => this.cargar()); }
}

@Component({
  selector: 'app-admin-shipping-page',
  imports: [FormsModule, CurrencyPipe, PanelTemplate, Card, Button],
  template: `
    <app-panel-template titulo="Tarifas de Envío Interciudad" subtitulo="Aplica por igual en ambos sentidos entre parejas de ciudades.">
      <app-card>
        <form (ngSubmit)="guardar()" class="form-grid">
          <input class="campo" placeholder="Ciudad A (ej. Armenia)" [(ngModel)]="ciudadA" name="ca" required />
          <input class="campo" placeholder="Ciudad B (ej. Pereira)" [(ngModel)]="ciudadB" name="cb" required />
          <input class="campo" type="number" placeholder="Tarifa COP" [(ngModel)]="tarifa" name="tar" required />
          <app-button tipo="submit" texto="Guardar Tarifa" />
        </form>
      </app-card>
      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>Ciudad A</th><th>Ciudad B</th><th>Tarifa</th></tr></thead>
          <tbody>
            @for (t of tarifas(); track t.id) {
              <tr><td>{{ t.ciudadA }}</td><td>{{ t.ciudadB }}</td><td>{{ t.tarifa | currency:'COP':'symbol-narrow':'1.0-0' }}</td></tr>
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
  private readonly ui = inject(NotificacionesUiService);

  protected readonly tarifas = signal<TarifaEnvioDTO[]>([]);
  protected ciudadA = ''; protected ciudadB = ''; protected tarifa = 12000;

  ngOnInit(): void { this.cargar(); }
  private cargar(): void { this.admin.listarTarifas().subscribe((t) => this.tarifas.set(t)); }

  protected guardar(): void {
    this.admin.guardarTarifa({ ciudadA: this.ciudadA, ciudadB: this.ciudadB, tarifa: this.tarifa }).subscribe({
      next: () => { this.ui.mostrar('Tarifa guardada y auditada.', 'exito'); this.cargar(); },
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}

@Component({
  selector: 'app-admin-reports-page',
  imports: [FormsModule, PanelTemplate, Card, Button],
  template: `
    <app-panel-template titulo="Generación de Reportes (PDF / CSV)">
      <app-card>
        <div class="form-grid">
          <select class="campo" [(ngModel)]="tipo">
            @for (t of tipos(); track t) { <option [value]="t">{{ t }}</option> }
          </select>
          <input type="date" class="campo" [(ngModel)]="desde" />
          <input type="date" class="campo" [(ngModel)]="hasta" />
          <app-button texto="Exportar PDF" (presionado)="descargar('PDF')" />
          <app-button texto="Exportar CSV" variante="secundario" (presionado)="descargar('CSV')" />
        </div>
      </app-card>
    </app-panel-template>
  `,
  styles: [`.form-grid { display: flex; gap: 0.75rem; flex-wrap: wrap; }`]
})
export class AdminReportsPage implements OnInit {
  private readonly admin = inject(AdminService);
  protected readonly tipos = signal<string[]>([]);
  protected tipo = 'PRESTAMOS'; protected desde = ''; protected hasta = '';

  ngOnInit(): void {
    this.admin.tiposReporte().subscribe((t) => { this.tipos.set(t); if (t[0]) this.tipo = t[0]; });
  }

  protected descargar(formato: 'PDF' | 'CSV'): void {
    this.admin.exportarReporte(this.tipo, formato, this.desde || undefined, this.hasta || undefined).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `reporte-${this.tipo.toLowerCase()}.${formato.toLowerCase()}`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }
}

@Component({
  selector: 'app-admin-payments-page',
  imports: [FormsModule, CurrencyPipe, DatePipe, PanelTemplate, Badge, Button],
  template: `
    <app-panel-template titulo="Historial Global de Pagos y Reembolsos">
      <div style="display: flex; gap: 0.75rem; margin-bottom: 1rem; flex-wrap: wrap;">
        <select class="campo" style="max-width: 200px;" [(ngModel)]="concepto">
          <option value="">Todos los conceptos</option>
          <option value="SUSCRIPCION">Suscripción</option>
          <option value="ENVIO_INTERCIUDAD">Envío Interciudad</option>
          <option value="MULTA">Multa</option>
          <option value="RECARGO">Recargo</option>
          <option value="RESERVA_SALA">Reserva Sala</option>
        </select>
        <select class="campo" style="max-width: 180px;" [(ngModel)]="estado">
          <option value="">Todos los estados</option>
          <option value="APROBADO">Aprobado</option>
          <option value="RECHAZADO">Rechazado</option>
          <option value="REEMBOLSADO">Reembolsado</option>
        </select>
        <app-button texto="Filtrar" (presionado)="buscar()" />
      </div>
      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>Referencia</th><th>Usuario ID</th><th>Concepto</th><th>Monto</th><th>Estado</th><th>Fecha</th></tr></thead>
          <tbody>
            @for (p of pagos(); track p.id) {
              <tr>
                <td>{{ p.referencia }}</td><td>#{{ p.usuarioId }}</td><td>{{ p.concepto }}</td>
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
  protected concepto = ''; protected estado = '';

  ngOnInit(): void { this.buscar(); }
  protected buscar(): void { this.admin.filtrarPagos(this.concepto, this.estado).subscribe((p) => this.pagos.set(p)); }
}

@Component({
  selector: 'app-admin-audit-page',
  imports: [DatePipe, PanelTemplate],
  template: `
    <app-panel-template titulo="Historial de Auditoría del Sistema">
      <div class="tabla-responsive">
        <table class="tabla">
          <thead><tr><th>Fecha/Hora</th><th>Módulo</th><th>Acción</th><th>Descripción</th><th>Usuario</th></tr></thead>
          <tbody>
            @for (a of logs(); track a.id) {
              <tr>
                <td>{{ a.fechaHora | date:'d MMM y, h:mm:ss a' }}</td>
                <td>{{ a.modulo }}</td><td>{{ a.accion }}</td><td>{{ a.descripcion }}</td><td>{{ a.usuarioCorreo || a.usuarioId }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `
})
export class AdminAuditPage implements OnInit {
  private readonly admin = inject(AdminService);
  protected readonly logs = signal<AuditoriaDTO[]>([]);
  ngOnInit(): void { this.admin.listarAuditoria().subscribe((l) => this.logs.set(l)); }
}

@Component({
  selector: 'app-admin-faq-page',
  imports: [FormsModule, PanelTemplate, Card, Button],
  template: `
    <app-panel-template titulo="Base de Conocimiento del Chatbot (FAQ)">
      <app-card>
        <form (ngSubmit)="crear()" class="form-grid">
          <input class="campo" placeholder="Pregunta frecuente *" [(ngModel)]="pregunta" name="pre" required />
          <input class="campo" placeholder="Respuesta *" [(ngModel)]="respuesta" name="res" required />
          <input class="campo" placeholder="Categoría" [(ngModel)]="categoria" name="cat" />
          <app-button tipo="submit" texto="Agregar al Chatbot" />
        </form>
      </app-card>
      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>Categoría</th><th>Pregunta</th><th>Respuesta</th><th>Acción</th></tr></thead>
          <tbody>
            @for (f of faqs(); track f.id) {
              <tr>
                <td>{{ f.categoria }}</td><td>{{ f.pregunta }}</td><td>{{ f.respuesta }}</td>
                <td><app-button texto="Eliminar" variante="peligro" tamano="pequeno" (presionado)="eliminar(f.id!)" /></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 0.65rem; }`]
})
export class AdminFaqPage implements OnInit {
  private readonly admin = inject(AdminService);
  protected readonly faqs = signal<PreguntaFrecuenteDTO[]>([]);
  protected pregunta = ''; protected respuesta = ''; protected categoria = 'GENERAL';

  ngOnInit(): void { this.cargar(); }
  private cargar(): void { this.admin.listarFaqs().subscribe((f) => this.faqs.set(f)); }

  protected crear(): void {
    this.admin.crearFaq({ pregunta: this.pregunta, respuesta: this.respuesta, categoria: this.categoria, activa: true }).subscribe(() => {
      this.pregunta = ''; this.respuesta = ''; this.cargar();
    });
  }
  protected eliminar(id: number): void { this.admin.eliminarFaq(id).subscribe(() => this.cargar()); }
}

@Component({
  selector: 'app-admin-policies-page',
  imports: [FormsModule, PanelTemplate, Card, Button],
  template: `
    <app-panel-template titulo="Políticas de Sanción, Multas y Plazos">
      <div class="grid-cards">
        <app-card>
          <h3>Políticas de Multa y Recargos</h3>
          @for (m of multas(); track $index) {
            <div style="margin-bottom: 0.75rem;">
              <strong>{{ m.concepto }}</strong>:
              <input type="number" class="campo" [(ngModel)]="m.monto" />
              <app-button texto="Guardar" tamano="pequeno" (presionado)="guardarMulta(m)" />
            </div>
          }
        </app-card>
        <app-card>
          <h3>Plazos del Sistema</h3>
          @for (p of plazos(); track $index) {
            <div style="margin-bottom: 0.75rem;">
              <strong>{{ p.clave }}</strong>:
              <input type="number" class="campo" [(ngModel)]="p.valor" />
              <app-button texto="Guardar" tamano="pequeno" (presionado)="guardarPlazo(p)" />
            </div>
          }
        </app-card>
      </div>
    </app-panel-template>
  `
})
export class AdminPoliciesPage implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly multas = signal<any[]>([]);
  protected readonly plazos = signal<any[]>([]);

  ngOnInit(): void {
    this.admin.obtenerPoliticasMulta().subscribe((m) => this.multas.set(m));
    this.admin.obtenerPlazos().subscribe((p) => this.plazos.set(p));
  }

  protected guardarMulta(m: any): void {
    this.admin.actualizarPoliticaMulta(m.concepto, m).subscribe(() => this.ui.mostrar('Política de multa actualizada.', 'exito'));
  }

  protected guardarPlazo(p: any): void {
    this.admin.actualizarPlazo(p.clave, p).subscribe(() => this.ui.mostrar('Plazo actualizado.', 'exito'));
  }
}
