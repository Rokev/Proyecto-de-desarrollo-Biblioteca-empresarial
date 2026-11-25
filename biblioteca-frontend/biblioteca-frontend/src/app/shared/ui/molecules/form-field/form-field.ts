import { Component, input } from '@angular/core';

@Component({
  selector: 'app-form-field',
  template: `
    <div class="fila" [class.fila--error]="error()">
      <label class="fila__label" [attr.for]="para()">
        {{ etiqueta() }}
        @if (obligatorio()) { <span class="fila__req" aria-hidden="true">*</span> }
      </label>
      <ng-content />
      @if (error()) { <small class="fila__error" role="alert">{{ error() }}</small> }
      @else if (ayuda()) { <small class="fila__ayuda">{{ ayuda() }}</small> }
    </div>
  `,
  styles: [`
    .fila { display: flex; flex-direction: column; gap: 0.35rem; margin-bottom: 0.9rem; }
    .fila__label { font-weight: 700; font-size: 0.9rem; }
    .fila__req { color: var(--color-error); }
    .fila__error { color: var(--color-error); font-weight: 600; font-size: 0.82rem; }
    .fila__ayuda { color: var(--color-texto-apoyo); font-size: 0.82rem; }
  `]
})
export class FormField {
  readonly etiqueta = input.required<string>();
  readonly para = input.required<string>();
  readonly obligatorio = input(false);
  readonly ayuda = input<string>();
  readonly error = input<string | null>(null);
}
