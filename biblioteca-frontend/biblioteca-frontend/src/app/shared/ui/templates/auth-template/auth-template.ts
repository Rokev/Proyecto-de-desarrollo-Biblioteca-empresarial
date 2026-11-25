import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-auth-template',
  imports: [RouterLink],
  template: `
    <a href="#contenido" class="skip-link">Saltar al contenido</a>
    <div class="auth-wrap">
      <header><a routerLink="/catalogo" class="logo">Biblioteca Red</a></header>
      <main id="contenido" class="auth-card"><ng-content /></main>
      <footer><small>Red de Bibliotecas Empresariales · 2026</small></footer>
    </div>
  `,
  styles: [`
    .auth-wrap { min-height: 100vh; display: flex; flex-direction: column; justify-content: space-between; align-items: center; padding: 2rem 1rem; }
    .logo { font-family: var(--font-heading); font-size: 1.75rem; font-weight: 700; color: var(--color-primario); text-decoration: none; }
    .auth-card { width: 100%; max-width: 480px; background: var(--color-superficie); padding: 2rem; border-radius: var(--radio-lg); border: 1px solid var(--color-borde); box-shadow: var(--sombra-md); }
    footer { color: var(--color-texto-apoyo); }
  `]
})
export class AuthTemplate {}
