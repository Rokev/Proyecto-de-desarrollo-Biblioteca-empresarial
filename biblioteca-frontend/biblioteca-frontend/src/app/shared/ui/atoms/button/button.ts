import { Component, input, output } from '@angular/core';

export type VarianteBoton = 'primario' | 'secundario' | 'contorno' | 'texto' | 'peligro';
export type TamanoBoton = 'normal' | 'pequeno';
export type TipoBoton = 'button' | 'submit';

@Component({
  selector: 'app-button',
  template: `
    <button
      [type]="tipo()"
      class="btn"
      [class.btn--secundario]="variante() === 'secundario'"
      [class.btn--contorno]="variante() === 'contorno'"
      [class.btn--texto]="variante() === 'texto'"
      [class.btn--peligro]="variante() === 'peligro'"
      [class.btn--sm]="tamano() === 'pequeno'"
      [disabled]="deshabilitado() || cargando()"
      (click)="presionado.emit()"
    >
      @if (cargando()) { <span class="btn__spinner" aria-hidden="true"></span> }
      <span>{{ texto() }}</span>
    </button>
  `,
  styles: [`
    :host { display: inline-block; }
    .btn {
      display: inline-flex; align-items: center; justify-content: center; gap: 0.45rem;
      padding: 0.55rem 1.15rem; font-family: var(--font-body); font-weight: 700;
      font-size: 0.95rem; border-radius: var(--radio); border: 1px solid transparent;
      background: var(--color-primario); color: #fff; cursor: pointer;
      transition: background 0.15s, opacity 0.15s;
      &:hover:not(:disabled) { background: var(--color-primario-hover); }
      &:disabled { opacity: 0.55; cursor: not-allowed; }
      &--secundario { background: var(--color-secundario); &:hover:not(:disabled) { background: var(--color-secundario-hover); } }
      &--contorno { background: transparent; border-color: var(--color-primario); color: var(--color-primario); &:hover:not(:disabled) { background: var(--color-primario-suave); } }
      &--texto { background: transparent; color: var(--color-primario); padding: 0.35rem 0.6rem; }
      &--peligro { background: var(--color-error); &:hover:not(:disabled) { filter: brightness(0.9); } }
      &--sm { padding: 0.35rem 0.75rem; font-size: 0.85rem; }
    }
    .btn__spinner {
      width: 14px; height: 14px; border: 2px solid currentColor;
      border-right-color: transparent; border-radius: 50%; animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class Button {
  readonly texto = input.required<string>();
  readonly variante = input<VarianteBoton>('primario');
  readonly tamano = input<TamanoBoton>('normal');
  readonly tipo = input<TipoBoton>('button');
  readonly deshabilitado = input(false);
  readonly cargando = input(false);
  readonly presionado = output<void>();
}
