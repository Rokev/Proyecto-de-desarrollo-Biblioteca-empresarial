import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { AuthTemplate } from '../../shared/ui/templates/auth-template/auth-template';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, AuthTemplate, FormField, Button, Alert],
  template: `
    <app-auth-template>
      <h1>Iniciar sesión</h1>
      @if (error()) { <app-alert titulo="Acceso denegado" [mensaje]="error()!" tipo="error" /> }
      <form [formGroup]="form" (ngSubmit)="entrar()">
        <app-form-field etiqueta="Correo electrónico" para="correo" [obligatorio]="true">
          <input id="correo" type="email" class="campo" formControlName="correo" />
        </app-form-field>
        <app-form-field etiqueta="Contraseña" para="pass" [obligatorio]="true">
          <input id="pass" type="password" class="campo" formControlName="contrasena" />
        </app-form-field>
        <div class="acciones">
          <app-button tipo="submit" texto="Ingresar" [cargando]="cargando()" />
        </div>
      </form>
      <div class="links">
        <a routerLink="/recuperar">¿Olvidaste tu contraseña?</a>
        <a routerLink="/registro">Crear una cuenta</a>
      </div>
    </app-auth-template>
  `,
  styles: [`
    .acciones { margin-top: 1rem; app-button, button { width: 100%; } }
    .links { display: flex; justify-content: space-between; margin-top: 1.25rem; font-size: 0.9rem; a { color: var(--color-primario); } }
  `]
})
export class LoginPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);

  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', [Validators.required]],
  });

  protected entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.cargando.set(false);
        this.auth.redirigirSegunRol();
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(extraerMensajeError(e));
      },
    });
  }
}
