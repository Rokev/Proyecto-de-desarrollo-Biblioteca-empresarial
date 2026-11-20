import { Component, input } from '@angular/core';
import { Navbar } from '../../organisms/navbar/navbar';

@Component({
  selector: 'app-main-template',
  imports: [Navbar],
  template: `
    <a href="#contenido" class="skip-link">Saltar al contenido</a>
    <div class="layout">
      <app-navbar />
      <main id="contenido" class="layout__main">
        @if (titulo()) {
          <header class="layout__head">
            <h1>{{ titulo() }}</h1>
            @if (subtitulo()) { <p class="layout__sub">{{ subtitulo() }}</p> }
          </header>
        }
        <ng-content />
      </main>
      <footer class="layout__foot">
        <div class="layout__foot-in">
          <span>Biblioteca Red · Sistema de Gestión</span>
          <span>2026</span>
        </div>
      </footer>
    </div>
  `,
  styles: [`
    .layout { min-height: 100vh; display: flex; flex-direction: column; }
    .layout__main { flex: 1; width: 100%; max-width: var(--max-width); margin: 0 auto; padding: 1.75rem 1.25rem; }
    .layout__head { margin-bottom: 1.5rem; h1 { margin-bottom: 0.25rem; } }
    .layout__sub { color: var(--color-texto-apoyo); margin: 0; }
    .layout__foot { background: var(--color-superficie); border-top: 1px solid var(--color-borde); padding: 1.1rem 1.25rem; color: var(--color-texto-apoyo); font-size: 0.9rem; }
    .layout__foot-in { max-width: var(--max-width); margin: 0 auto; display: flex; justify-content: space-between; }
  `]
})
export class MainTemplate {
  readonly titulo = input<string>();
  readonly subtitulo = input<string>();
}
