import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { AuthTemplate } from '../../shared/ui/templates/auth-template/auth-template';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, AuthTemplate, FormField, Button, Alert],
  template: `
    <app-auth-template>
      <h1>Crear cuenta</h1>
      @if (error()) { <app-alert titulo="Error en registro" [mensaje]="error()!" tipo="error" /> }
      <form [formGroup]="form" (ngSubmit)="registrar()">
        <app-form-field etiqueta="Nombre" para="nom" [obligatorio]="true">
          <input id="nom" class="campo" formControlName="nombre" />
        </app-form-field>
        <app-form-field etiqueta="Apellido" para="ape" [obligatorio]="true">
          <input id="ape" class="campo" formControlName="apellido" />
        </app-form-field>
        <app-form-field etiqueta="Documento de identidad" para="doc" [obligatorio]="true">
          <input id="doc" class="campo" formControlName="documento" />
        </app-form-field>
        <app-form-field etiqueta="Nombre de usuario único" para="user" [obligatorio]="true">
          <input id="user" class="campo" formControlName="nombreUsuario" />
        </app-form-field>
        <app-form-field etiqueta="Correo electrónico" para="correo" [obligatorio]="true">
          <input id="correo" type="email" class="campo" formControlName="correo" />
        </app-form-field>
        <app-form-field etiqueta="Contraseña (mínimo 8 caracteres)" para="pass" [obligatorio]="true">
          <input id="pass" type="password" class="campo" formControlName="contrasena" />
        </app-form-field>
        <app-form-field etiqueta="Dirección" para="dir">
          <input id="dir" class="campo" formControlName="direccion" />
        </app-form-field>
        <app-button tipo="submit" texto="Registrarme" [cargando]="cargando()" />
      </form>
      <p class="pie"><a routerLink="/login">¿Ya tienes cuenta? Inicia sesión</a></p>
    </app-auth-template>
  `,
  styles: [`.pie { margin-top: 1rem; text-align: center; a { color: var(--color-primario); } }`]
})
export class RegisterPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);
  private readonly router = inject(Router);

  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    nombre: ['', Validators.required],
    apellido: ['', Validators.required],
    documento: ['', Validators.required],
    nombreUsuario: ['', Validators.required],
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', [Validators.required, Validators.minLength(8)]],
    direccion: [''],
  });

  protected registrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Verifica los campos obligatorios (contraseña mínimo 8 caracteres).');
      return;
    }
    this.cargando.set(true);
    this.error.set(null);
    this.auth.registrar(this.form.getRawValue()).subscribe({
      next: () => {
        this.cargando.set(false);
        this.ui.mostrar('Cuenta creada con éxito. Ahora puedes iniciar sesión.', 'exito');
        this.router.navigate(['/login']);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(extraerMensajeError(e));
      },
    });
  }
}
