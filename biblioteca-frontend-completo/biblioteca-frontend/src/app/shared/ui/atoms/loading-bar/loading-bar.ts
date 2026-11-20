import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading-bar',
  template: `@if (activa()) { <div class="barra" role="progressbar" aria-label="Cargando"></div> }`,
  styles: [`
    .barra {
      position: fixed; inset: 0 0 auto 0; height: 3px; background: var(--color-primario-suave); z-index: 9999; overflow: hidden;
      &::after { content: ''; position: absolute; inset: 0; width: 40%; background: var(--color-primario); animation: slide 1.1s ease-in-out infinite; }
    }
    @keyframes slide { from { transform: translateX(-100%); } to { transform: translateX(260%); } }
  `]
})
export class LoadingBar {
  readonly activa = input(false);
}
