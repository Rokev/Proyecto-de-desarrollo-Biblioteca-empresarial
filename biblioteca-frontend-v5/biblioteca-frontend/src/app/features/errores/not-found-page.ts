import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { EmptyState } from '../../shared/ui/molecules/empty-state/empty-state';
import { MainTemplate } from '../../shared/ui/templates/main-template/main-template';

@Component({
  selector: 'app-not-found-page',
  imports: [MainTemplate, EmptyState],
  template: `
    <app-main-template>
      <app-empty-state
        icono="🧭"
        titulo="Página no disponible"
        descripcion="La ruta solicitada no existe o no tienes permisos suficientes para acceder a ella."
        textoAccion="Volver al catálogo"
        (accion)="volver()"
      />
    </app-main-template>
  `
})
export class NotFoundPage {
  private readonly router = inject(Router);
  protected volver(): void { this.router.navigate(['/catalogo']); }
}
