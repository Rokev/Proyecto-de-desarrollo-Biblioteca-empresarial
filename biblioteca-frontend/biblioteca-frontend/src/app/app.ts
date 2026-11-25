import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CargaService } from './core/services/carga.service';
import { NotificacionesUiService } from './core/services/notificaciones-ui.service';
import { LoadingBar } from './shared/ui/atoms/loading-bar/loading-bar';
import { ChatbotWidget } from './shared/ui/organisms/chatbot-widget/chatbot-widget';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LoadingBar, ChatbotWidget],
  template: `
    <app-loading-bar [activa]="carga.activa()" />

    <div class="toast-stack" aria-live="polite">
      @for (t of notificaciones.toasts(); track t.id) {
        <div class="toast" [class]="'toast toast--' + t.tipo" role="alert">
          <span>{{ t.texto }}</span>
          <button type="button" aria-label="Cerrar aviso" (click)="notificaciones.cerrar(t.id)">×</button>
        </div>
      }
    </div>

    <router-outlet />
    <app-chatbot-widget />
  `,
  styles: [`
    .toast-stack { position: fixed; bottom: 1.25rem; right: 1.25rem; z-index: 9000; display: flex; flex-direction: column; gap: 0.5rem; max-width: 380px; }
    .toast { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.85rem 1rem; border-radius: var(--radio); color: #fff; box-shadow: var(--sombra-md); font-weight: 600; font-size: 0.9rem;
      button { background: transparent; border: none; color: inherit; font-size: 1.2rem; cursor: pointer; } }
    .toast--info { background: var(--color-info); }
    .toast--exito { background: var(--color-exito); }
    .toast--advertencia { background: var(--color-advertencia); }
    .toast--error { background: var(--color-error); }
  `]
})
export class App {
  protected readonly carga = inject(CargaService);
  protected readonly notificaciones = inject(NotificacionesUiService);
}
