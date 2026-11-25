import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../../core/services/admin.service';
import { Button } from '../../atoms/button/button';

interface MensajeChat { emisor: 'usuario' | 'bot'; texto: string; sugerencias?: string[]; }

@Component({
  selector: 'app-chatbot-widget',
  imports: [FormsModule, Button],
  template: `
    <div class="chat-widget">
      @if (abierto()) {
        <section class="chat-box" aria-label="Asistente Virtual">
          <header class="chat-box__head">
            <strong>Asistente Biblioteca</strong>
            <button type="button" (click)="abierto.set(false)" aria-label="Cerrar chat">×</button>
          </header>

          <div class="chat-box__body">
            @for (m of mensajes(); track $index) {
              <div class="bubble" [class.bubble--user]="m.emisor === 'usuario'">
                <p>{{ m.texto }}</p>
                @if (m.sugerencias?.length) {
                  <div class="chips">
                    @for (s of m.sugerencias; track s) {
                      <button type="button" class="chip" (click)="enviarTexto(s)">{{ s }}</button>
                    }
                  </div>
                }
              </div>
            }
          </div>

          <footer class="chat-box__foot">
            <form (ngSubmit)="enviar()" class="chat-form">
              <input class="campo" placeholder="Escribe tu consulta..." [(ngModel)]="texto" name="msg" />
              <app-button tipo="submit" texto="Enviar" tamano="pequeno" />
            </form>
          </footer>
        </section>
      }

      <button type="button" class="chat-fab" (click)="abierto.update(v => !v)" aria-label="Abrir asistente">
        💬 Ayuda
      </button>
    </div>
  `,
  styles: [`
    .chat-widget { position: fixed; bottom: 3.75rem; left: 1.25rem; z-index: 8000; }
    .chat-fab { background: var(--color-primario); color: #fff; border: none; border-radius: 999px; padding: 0.7rem 1.2rem; font-weight: 700; cursor: pointer; box-shadow: var(--sombra-md); }
    .chat-box { width: 340px; max-width: calc(100vw - 2rem); background: var(--color-superficie); border: 1px solid var(--color-borde); border-radius: var(--radio-lg); box-shadow: var(--sombra-md); margin-bottom: 0.75rem; display: flex; flex-direction: column; overflow: hidden; }
    .chat-box__head { background: var(--color-primario); color: #fff; padding: 0.75rem 1rem; display: flex; justify-content: space-between; button { background: none; border: none; color: #fff; font-size: 1.25rem; cursor: pointer; } }
    .chat-box__body { padding: 0.85rem; max-height: 300px; overflow-y: auto; display: flex; flex-direction: column; gap: 0.6rem; }
    .bubble { background: #f1f5f9; padding: 0.6rem 0.85rem; border-radius: var(--radio); font-size: 0.9rem; p { margin: 0; } &--user { background: var(--color-primario-suave); align-self: flex-end; } }
    .chips { display: flex; flex-wrap: wrap; gap: 0.35rem; margin-top: 0.5rem; }
    .chip { background: #fff; border: 1px solid var(--color-primario); color: var(--color-primario); border-radius: 999px; padding: 0.2rem 0.6rem; font-size: 0.78rem; cursor: pointer; }
    .chat-box__foot { padding: 0.65rem; border-top: 1px solid var(--color-borde); background: #f8fafc; }
    .chat-form { display: flex; gap: 0.4rem; }
  `]
})
export class ChatbotWidget {
  private readonly admin = inject(AdminService);

  protected readonly abierto = signal(false);
  protected readonly mensajes = signal<MensajeChat[]>([
    { emisor: 'bot', texto: '¡Hola! Soy el asistente de Biblioteca Red. ¿En qué puedo ayudarte hoy?' }
  ]);
  protected texto = '';

  protected enviar(): void {
    const q = this.texto.trim();
    if (!q) return;
    this.texto = '';
    this.enviarTexto(q);
  }

  protected enviarTexto(q: string): void {
    this.mensajes.update((m) => [...m, { emisor: 'usuario', texto: q }]);
    this.admin.enviarMensajeChatbot(q).subscribe({
      next: (res) => {
        this.mensajes.update((m) => [
          ...m,
          { emisor: 'bot', texto: res.respuesta, sugerencias: res.sugerencias }
        ]);
      },
      error: () => {
        this.mensajes.update((m) => [
          ...m,
          { emisor: 'bot', texto: 'No pude procesar tu consulta en este momento.' }
        ]);
      }
    });
  }
}
