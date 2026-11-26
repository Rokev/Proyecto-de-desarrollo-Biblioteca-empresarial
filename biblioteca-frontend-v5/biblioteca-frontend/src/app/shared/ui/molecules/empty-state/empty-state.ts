import { Component, input, output } from '@angular/core';
import { Button } from '../../atoms/button/button';

@Component({
  selector: 'app-empty-state',
  imports: [Button],
  template: `
    <div class="vacio">
      <div class="vacio__icono" aria-hidden="true">{{ icono() }}</div>
      <h2 class="vacio__titulo">{{ titulo() }}</h2>
      <p class="vacio__desc">{{ descripcion() }}</p>
      @if (textoAccion()) {
        <app-button [texto]="textoAccion()!" (presionado)="accion.emit()" />
      }
    </div>
  `,
  styles: [`
    .vacio { text-align: center; padding: 3rem 1.5rem; background: var(--color-superficie); border: 1px dashed var(--color-borde); border-radius: var(--radio-lg); }
    .vacio__icono { font-size: 2.5rem; margin-bottom: 0.75rem; }
    .vacio__titulo { font-size: 1.25rem; margin-bottom: 0.4rem; }
    .vacio__desc { color: var(--color-texto-apoyo); max-width: 440px; margin: 0 auto 1.25rem; }
  `]
})
export class EmptyState {
  readonly icono = input('📚');
  readonly titulo = input.required<string>();
  readonly descripcion = input.required<string>();
  readonly textoAccion = input<string>();
  readonly accion = output<void>();
}
