import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, concatMap, from, map, of, toArray } from 'rxjs';
import { EjemplarDTO, LibroDTO, SedeDTO } from '../../core/models/biblioteca.models';
import { AuthService } from '../../core/services/auth.service';
import { CatalogoService } from '../../core/services/catalogo.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Badge } from '../../shared/ui/atoms/badge/badge';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { PanelTemplate } from '../../shared/ui/templates/panel-template/panel-template';

const MAX_CANTIDAD = 100;

@Component({
  selector: 'app-staff-copies-page',
  imports: [FormsModule, PanelTemplate, Card, Badge, Button, FormField],
  template: `
    <app-panel-template titulo="Gestión de Ejemplares y Stock por Sede" subtitulo="Registra copias físicas o actualiza su disponibilidad.">
      <app-card>
        <h3>Registrar Nuevo Ejemplar (Aumentar Stock)</h3>
        <form (ngSubmit)="crear()" class="form-grid">
          <app-form-field etiqueta="Libro" para="libSel" [obligatorio]="true">
            <select id="libSel" class="campo" [(ngModel)]="libroId" name="libSel">
              @for (l of libros(); track l.id) { <option [ngValue]="l.id">{{ l.titulo }} ({{ l.isbn }})</option> }
            </select>
          </app-form-field>

          <app-form-field etiqueta="Nombre de la Sede" para="sedeNombre" [obligatorio]="true"
                          ayuda="Escribe el nombre o elígelo de la lista.">
            <input id="sedeNombre" class="campo" name="sedeNombre" list="listaSedes" autocomplete="off"
                   placeholder="Ej. Sede Central"
                   [ngModel]="sedeNombre" (ngModelChange)="alEscribirNombre($event)" />
            <datalist id="listaSedes">
              @for (s of sedes(); track s.id) { <option [value]="s.nombre">{{ s.ciudad }} · ID {{ s.id }}</option> }
            </datalist>
          </app-form-field>

          <app-form-field etiqueta="ID de la Sede" para="sedeIdInput" [obligatorio]="true"
                          [error]="errorSede()" [ayuda]="sedeEncontrada()?.ciudad ?? 'Se completa solo al elegir la sede.'">
            <input id="sedeIdInput" class="campo" type="number" min="1" name="sedeIdInput" placeholder="Ej. 1"
                   [ngModel]="sedeId" (ngModelChange)="alEscribirId($event)" />
          </app-form-field>

          <app-form-field etiqueta="Cantidad a sumar" para="cant" [obligatorio]="true"
                          [ayuda]="resumenStock()">
            <input id="cant" class="campo" type="number" min="1" [max]="maxCantidad" name="cant"
                   [(ngModel)]="cantidad" />
          </app-form-field>

          <app-form-field etiqueta="Código de Barras / Inventario" para="cod" [obligatorio]="true"
                          [ayuda]="cantidad > 1 ? 'Se usará como prefijo: ' + (codigoBarras.trim() || 'EJ') + '-001, -002…' : ''">
            <input id="cod" class="campo" [(ngModel)]="codigoBarras" name="cod" placeholder="Ej. EJ-00101" required />
          </app-form-field>

          <app-form-field etiqueta="Estado Inicial" para="est">
            <select id="est" class="campo" [(ngModel)]="estado" name="est">
              <option value="DISPONIBLE">DISPONIBLE</option>
              <option value="RESERVADO">RESERVADO</option>
              <option value="DANADO">DANADO</option>
            </select>
          </app-form-field>

          <div style="align-self: end; margin-bottom: 0.9rem;">
            <app-button tipo="submit" [texto]="guardando() ? 'Registrando…' : 'Registrar Ejemplar'" [deshabilitado]="guardando()" />
          </div>
        </form>
      </app-card>

      <div class="tabla-responsive" style="margin-top: 1.5rem;">
        <table class="tabla">
          <thead><tr><th>ID</th><th>Código</th><th>Libro</th><th>Sede</th><th>Estado</th><th>Cambiar Estado / Stock</th></tr></thead>
          <tbody>
            @for (e of ejemplares(); track e.id) {
              <tr>
                <td>{{ e.id }}</td>
                <td><strong>{{ e.codigoBarras }}</strong></td>
                <td>{{ nombreLibro(e.libroId) }}</td>
                <td>{{ nombreSede(e.sedeId) }}</td>
                <td><app-badge [texto]="e.estado" [estado]="e.estado" /></td>
                <td style="display: flex; gap: 0.4rem;">
                  @if (e.estado !== 'DISPONIBLE') {
                    <app-button texto="Marcar Disponible" tamano="pequeno" (presionado)="cambiarEstado(e, 'DISPONIBLE')" />
                  }
                  @if (e.estado === 'DISPONIBLE') {
                    <app-button texto="Marcar Dañado" variante="peligro" tamano="pequeno" (presionado)="cambiarEstado(e, 'DANADO')" />
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </app-panel-template>
  `,
  styles: [`.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 0.75rem; }`]
})
export class StaffCopiesPage implements OnInit {
  private readonly catalogo = inject(CatalogoService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected readonly maxCantidad = MAX_CANTIDAD;
  protected readonly ejemplares = signal<EjemplarDTO[]>([]);
  protected readonly libros = signal<LibroDTO[]>([]);
  protected readonly sedes = signal<SedeDTO[]>([]);
  protected readonly guardando = signal(false);

  protected libroId = 0;
  protected sedeNombre = '';
  protected sedeId: number | null = null;
  protected cantidad = 1;
  protected codigoBarras = '';
  protected estado: EjemplarDTO['estado'] = 'DISPONIBLE';

  ngOnInit(): void {
    this.cargar();
    this.catalogo.buscarLibros().subscribe((l) => {
      this.libroId = l[0]?.id ?? 0;
      this.libros.set(l);
    });
    // Si /sedes no está permitido para este rol, se usa /sedes/activas como respaldo.
    this.catalogo.listarTodasSedes()
      .pipe(catchError(() => this.catalogo.listarSedesActivas()))
      .subscribe((s) => {
        const propia = s.find((x) => x.id === this.auth.perfil()?.sedeId);
        if (propia) {
          this.sedeId = propia.id;
          this.sedeNombre = propia.nombre;
        }
        this.sedes.set(s);
      });
  }

  private cargar(): void {
    this.catalogo.listarEjemplares().subscribe((e) => this.ejemplares.set(e));
  }

  private normalizar(t: string): string {
    return t.normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();
  }

  protected sedeEncontrada(): SedeDTO | undefined {
    return this.sedeId ? this.sedes().find((s) => s.id === Number(this.sedeId)) : undefined;
  }

  protected errorSede(): string | null {
    if (this.sedeId && this.sedes().length > 0 && !this.sedeEncontrada()) return 'No existe una sede con ese ID.';
    return null;
  }

  protected alEscribirNombre(valor: string): void {
    this.sedeNombre = valor;
    const n = this.normalizar(valor);
    const coincide = n ? this.sedes().find((s) => this.normalizar(s.nombre) === n) : undefined;
    if (coincide) this.sedeId = coincide.id;
  }

  protected alEscribirId(valor: number | null): void {
    this.sedeId = valor;
    const sede = valor ? this.sedes().find((s) => s.id === Number(valor)) : undefined;
    if (sede) this.sedeNombre = sede.nombre;
  }

  /** Resuelve la sede a partir del ID o, si no hay ID, del nombre (coincidencia exacta o única). */
  private resolverSede(): number | null {
    if (this.sedeId) return Number(this.sedeId);
    const n = this.normalizar(this.sedeNombre);
    if (!n) return null;
    const lista = this.sedes();
    const exacta = lista.find((s) => this.normalizar(s.nombre) === n);
    if (exacta) return exacta.id;
    const parciales = lista.filter((s) => this.normalizar(s.nombre).includes(n));
    return parciales.length === 1 ? parciales[0].id : null;
  }

  protected resumenStock(): string {
    const sedeId = this.sedeEncontrada()?.id;
    if (!this.libroId || !sedeId) return `Máximo ${MAX_CANTIDAD} por registro.`;
    const copias = this.ejemplares().filter((e) => e.libroId === Number(this.libroId) && e.sedeId === sedeId);
    const disp = copias.filter((e) => e.estado === 'DISPONIBLE').length;
    const suma = Math.max(Number(this.cantidad) || 0, 0);
    return `Stock actual en la sede: ${copias.length} (${disp} disponibles) → quedará en ${copias.length + suma}.`;
  }

  protected nombreLibro(id: number): string {
    return this.libros().find((l) => l.id === id)?.titulo ?? `Libro #${id}`;
  }

  protected nombreSede(id: number): string {
    return this.sedes().find((s) => s.id === id)?.nombre ?? `Sede #${id}`;
  }

  private generarCodigos(base: string, n: number): string[] {
    const usados = new Set(this.ejemplares().map((e) => e.codigoBarras.toLowerCase()));
    if (n === 1) return [base];
    const codigos: string[] = [];
    for (let k = 1; codigos.length < n; k++) {
      const c = `${base}-${String(k).padStart(3, '0')}`;
      if (!usados.has(c.toLowerCase())) codigos.push(c);
    }
    return codigos;
  }

  protected crear(): void {
    const base = this.codigoBarras.trim();
    const sedeId = this.resolverSede();
    const n = Math.floor(Number(this.cantidad));

    if (!base || !this.libroId || (!this.sedeNombre.trim() && !this.sedeId)) {
      this.ui.mostrar('Completa libro, sede (nombre e ID), cantidad y código de barras.', 'advertencia');
      return;
    }
    if (!sedeId) {
      this.ui.mostrar('No se encontró esa sede. Elige una de la lista o escribe su ID.', 'advertencia');
      return;
    }
    if (this.sedes().length > 0 && !this.sedes().some((s) => s.id === sedeId)) {
      this.ui.mostrar('El ID de la sede no existe.', 'advertencia');
      return;
    }
    if (!Number.isFinite(n) || n < 1 || n > MAX_CANTIDAD) {
      this.ui.mostrar(`La cantidad debe estar entre 1 y ${MAX_CANTIDAD}.`, 'advertencia');
      return;
    }
    if (n === 1 && this.ejemplares().some((e) => e.codigoBarras.toLowerCase() === base.toLowerCase())) {
      this.ui.mostrar('Ya existe un ejemplar con ese código de barras.', 'advertencia');
      return;
    }

    const dtos: EjemplarDTO[] = this.generarCodigos(base, n).map((codigoBarras) => ({
      codigoBarras,
      libroId: Number(this.libroId),
      sedeId,
      estado: this.estado,
    }));

    this.guardando.set(true);
    from(dtos).pipe(
      concatMap((dto) => this.catalogo.crearEjemplar(dto).pipe(
        map(() => ({ ok: true as const, error: '' })),
        catchError((e) => of({ ok: false as const, error: extraerMensajeError(e) })),
      )),
      toArray(),
    ).subscribe((res) => {
      this.guardando.set(false);
      const ok = res.filter((r) => r.ok).length;
      const fallos = res.filter((r) => !r.ok);
      if (ok > 0) {
        this.ui.mostrar(`Se sumaron ${ok} ejemplar(es) al stock de ${this.nombreSede(sedeId)}.`, 'exito');
        this.codigoBarras = '';
        this.cantidad = 1;
      }
      if (fallos.length > 0) {
        this.ui.mostrar(`${fallos.length} ejemplar(es) no se pudieron registrar: ${fallos[0].error}`, 'error');
      }
      this.cargar();
    });
  }

  protected cambiarEstado(e: EjemplarDTO, nuevoEstado: EjemplarDTO['estado']): void {
    this.catalogo.actualizarEjemplar(e.id!, { ...e, estado: nuevoEstado }).subscribe({
      next: () => {
        this.ui.mostrar(`Estado actualizado a ${nuevoEstado}.`, 'exito');
        this.cargar();
      },
      error: (err) => this.ui.mostrar(extraerMensajeError(err), 'error'),
    });
  }
}