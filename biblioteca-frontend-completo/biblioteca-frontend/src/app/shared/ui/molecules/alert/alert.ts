import { Component, input } from '@angular/core';

@Component({
  selector: 'app-alert',
  template: `
    <div class="alerta" [class]="'alerta alerta--' + tipo()" [attr.role]="tipo() === 'error' ? 'alert' : 'status'">
      <strong class="alerta__titulo">{{ titulo() }}</strong>
      @if (mensaje()) { <p class="alerta__texto">{{ mensaje() }}</p> }
      <ng-content />
    </div>
  `,
  styles: [`
    .alerta { padding: 0.9rem 1.1rem; border-radius: var(--radio); border-left: 4px solid; margin-bottom: 1rem; }
    .alerta--info { background: var(--color-info-bg); border-color: var(--color-info); color: #0c4a6e; }
    .alerta--exito { background: var(--color-exito-bg); border-color: var(--color-exito); color: #14532d; }
    .alerta--advertencia { background: var(--color-advertencia-bg); border-color: var(--color-advertencia); color: #78350f; }
    .alerta--error { background: var(--color-error-bg); border-color: var(--color-error); color: #7f1d1d; }
    .alerta__titulo { display: block; margin-bottom: 0.2rem; }
    .alerta__texto { margin: 0; font-size: 0.92rem; }
  `]
})
export class Alert {
  readonly titulo = input.required<string>();
  readonly mensaje = input<string>();
  readonly tipo = input<'info' | 'exito' | 'advertencia' | 'error'>('info');
}
