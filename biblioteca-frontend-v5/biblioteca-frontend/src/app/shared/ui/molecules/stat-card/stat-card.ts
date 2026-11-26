import { Component, input } from '@angular/core';
import { Card } from '../../atoms/card/card';

@Component({
  selector: 'app-stat-card',
  imports: [Card],
  template: `
    <app-card [destacada]="alerta()">
      <span class="stat__etiqueta">{{ etiqueta() }}</span>
      <strong class="stat__valor" [class.stat__valor--alerta]="alerta()">{{ valor() }}</strong>
    </app-card>
  `,
  styles: [`
    .stat__etiqueta { display: block; font-size: 0.85rem; color: var(--color-texto-apoyo); font-weight: 600; }
    .stat__valor { display: block; font-family: var(--font-heading); font-size: 1.75rem; margin-top: 0.25rem; }
    .stat__valor--alerta { color: var(--color-error); }
  `]
})
export class StatCard {
  readonly etiqueta = input.required<string>();
  readonly valor = input.required<string | number>();
  readonly alerta = input(false);
}
