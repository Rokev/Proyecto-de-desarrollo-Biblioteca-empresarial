import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from '../../atoms/button/button';

@Component({
  selector: 'app-confirm-dialog',
  imports: [FormsModule, Button],
  template: `
    @if (abierto()) {
      <div class="backdrop" (click)="cerrar()">
        <div class="dialog" role="dialog" aria-modal="true" [attr.aria-label]="titulo()" (click)="$event.stopPropagation()">
          <h3>{{ titulo() }}</h3>
          <p>{{ mensaje() }}</p>
          @if (mostrarInput()) {
            <label class="dialog__label" for="confirmMotivo">{{ etiquetaInput() }}</label>
            <input id="confirmMotivo" class="campo" [(ngModel)]="valorInput" name="motivo" />
          }
          <div class="dialog__btns">
            <app-button texto="Cancelar" variante="contorno" (presionado)="cerrar()" />
            <app-button [texto]="textoConfirmar()" [variante]="peligro() ? 'peligro' : 'primario'" (presionado)="onConfirm()" />
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.55); display: flex; align-items: center; justify-content: center; z-index: 9500; padding: 1rem; }
    .dialog { background: var(--color-superficie); border-radius: var(--radio-lg); padding: 1.5rem; max-width: 460px; width: 100%; box-shadow: var(--sombra-md); h3 { margin-bottom: 0.5rem; } p { color: var(--color-texto-apoyo); margin-bottom: 1rem; white-space: pre-line; } }
    .dialog__label { display: block; font-weight: 600; margin-bottom: 0.35rem; font-size: 0.9rem; }
    .dialog .campo { width: 100%; margin-bottom: 1rem; }
    .dialog__btns { display: flex; justify-content: flex-end; gap: 0.65rem; }
  `]
})
export class ConfirmDialog {
  readonly abierto = model(false);
  readonly titulo = input.required<string>();
  readonly mensaje = input.required<string>();
  readonly textoConfirmar = input('Confirmar');
  readonly peligro = input(false);
  readonly mostrarInput = input(false);
  readonly etiquetaInput = input('Motivo');
  readonly confirmar = output<string>();

  protected valorInput = '';

  protected cerrar(): void {
    this.abierto.set(false);
    this.valorInput = '';
  }

  protected onConfirm(): void {
    const valor = this.valorInput;
    this.abierto.set(false);
    this.valorInput = '';
    this.confirmar.emit(valor);
  }
}