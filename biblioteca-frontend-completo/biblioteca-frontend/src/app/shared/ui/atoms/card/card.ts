import { Component, input } from '@angular/core';

@Component({
  selector: 'app-card',
  template: `
    <section class="card" [class.card--destacada]="destacada()">
      <header class="card__encabezado"><ng-content select="[cardEncabezado]" /></header>
      <div class="card__cuerpo"><ng-content /></div>
      <footer class="card__pie"><ng-content select="[cardPie]" /></footer>
    </section>
  `,
  styles: [`
    .card {
      background: var(--color-superficie); border: 1px solid var(--color-borde);
      border-radius: var(--radio-lg); box-shadow: var(--sombra-sm);
      overflow: hidden; display: flex; flex-direction: column;
    }
    .card--destacada { border-color: var(--color-primario); box-shadow: var(--sombra-md); }
    .card__encabezado { padding: 0.9rem 1.25rem; border-bottom: 1px solid var(--color-borde); &:empty { display: none; } }
    .card__cuerpo { padding: 1.25rem; flex: 1; }
    .card__pie { padding: 0.85rem 1.25rem; background: #f8fafc; border-top: 1px solid var(--color-borde); display: flex; gap: 0.5rem; flex-wrap: wrap; &:empty { display: none; } }
  `]
})
export class Card {
  readonly destacada = input(false);
}
