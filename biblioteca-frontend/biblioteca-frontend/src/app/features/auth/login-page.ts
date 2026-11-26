import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
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
      <form [formGroup]="form" (ngSubmit)="entrar()" novalidate>
        <app-form-field etiqueta="Correo electrónico" para="correo" [obligatorio]="true" [error]="errorCorreo()">
          <input id="correo" type="email" class="campo" formControlName="correo" />
        </app-form-field>
        <app-form-field etiqueta="Contraseña" para="pass" [obligatorio]="true" [error]="errorPass()">
          <input id="pass" type="password" class="campo" formControlName="contrasena" />
        </app-form-field>
        <div class="acciones">
          <app-button tipo="submit" texto="Ingresar" [cargando]="cargando()" />
        </div>
      </form>
      <div class="links">
        <a href="" (click)="alternarRecuperar($event)">¿Olvidaste tu contraseña?</a>
        <a routerLink="/registro">Crear una cuenta</a>
      </div>

      @if (recuperando()) {
        <section class="recuperar">
          <h2>Recuperar contraseña</h2>
          @if (errorRec()) { <app-alert titulo="No se pudo solicitar" [mensaje]="errorRec()!" tipo="error" /> }
          @if (mensajeRec()) { <app-alert titulo="Solicitud enviada" [mensaje]="mensajeRec()!" tipo="exito" /> }

          @if (!tokenGenerado()) {
            <form [formGroup]="formRec" (ngSubmit)="solicitarToken()" novalidate>
              <app-form-field etiqueta="Correo registrado" para="correoRec" [obligatorio]="true" [error]="errorCorreoRec()">
                <input id="correoRec" type="email" class="campo" formControlName="correo" />
              </app-form-field>
              <div class="acciones">
                <app-button tipo="submit" texto="Enviar token de recuperación" [cargando]="cargandoRec()" />
              </div>
            </form>
          } @else {
            <div class="token-box">
              <p>Simulamos el envío al correo. Este es tu token de recuperación (válido 24 horas, un solo uso):</p>
              <code>{{ tokenGenerado() }}</code>
              <div class="acciones">
                <app-button texto="Continuar y cambiar contraseña" (presionado)="irARestablecer()" />
              </div>
            </div>
          }
        </section>
      }
    </app-auth-template>
  `,
  styles: [`
    .acciones { margin-top: 1rem; app-button, button { width: 100%; } }
    .recuperar { margin-top: 1.5rem; padding-top: 1.25rem; border-top: 1px solid var(--color-borde); h2 { font-size: 1.1rem; margin: 0 0 0.75rem; } }
    .token-box { code { display: block; margin: 0.5rem 0; padding: 0.75rem; background: var(--color-superficie); border: 1px dashed var(--color-borde); border-radius: var(--radio-lg); word-break: break-all; font-weight: 700; } }
    .links { display: flex; justify-content: space-between; margin-top: 1.25rem; font-size: 0.9rem; a { color: var(--color-primario); } }
  `]
})
export class LoginPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly recuperando = signal(false);
  protected readonly cargandoRec = signal(false);
  protected readonly errorRec = signal<string | null>(null);
  protected readonly mensajeRec = signal<string | null>(null);
  protected readonly tokenGenerado = signal<string | null>(null);
  // Debe ir dentro de un FormGroup ([formGroup]) para que (ngSubmit) se dispare con ReactiveFormsModule
  protected readonly formRec = this.fb.group({
    correo: ['', [Validators.required, Validators.email]],
  });
  protected get correoRec() { return this.formRec.controls.correo; }

  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', [Validators.required]],
  });

  protected errorCorreo(): string | null {
    const c = this.form.controls.correo;
    if (!c.touched || !c.errors) return null;
    if (c.errors['required']) return 'El correo es obligatorio.';
    if (c.errors['email']) return 'Ingresa un correo electrónico válido.';
    return null;
  }

  protected errorPass(): string | null {
    const c = this.form.controls.contrasena;
    if (!c.touched || !c.errors) return null;
    return c.errors['required'] ? 'La contraseña es obligatoria.' : null;
  }

  protected errorCorreoRec(): string | null {
    const c = this.correoRec;
    if (!c.touched || !c.errors) return null;
    return c.errors['required'] ? 'El correo es obligatorio.' : 'Ingresa un correo electrónico válido.';
  }

  protected alternarRecuperar(evento: Event): void {
    evento.preventDefault();
    this.recuperando.update((v) => !v);
    this.errorRec.set(null);
    this.mensajeRec.set(null);
    this.tokenGenerado.set(null);
  }

  protected solicitarToken(): void {
    if (this.correoRec.invalid) {
      this.correoRec.markAsTouched();
      return;
    }
    this.cargandoRec.set(true);
    this.errorRec.set(null);
    this.mensajeRec.set(null);
    this.auth.solicitarRecuperacion(this.correoRec.value.trim()).subscribe({
      next: (r) => {
        this.cargandoRec.set(false);
        if (r.token) {
          this.tokenGenerado.set(r.token);
        } else {
          this.mensajeRec.set(r.mensaje);
        }
      },
      error: (e) => {
        this.cargandoRec.set(false);
        this.errorRec.set(extraerMensajeError(e));
      },
    });
  }

  protected irARestablecer(): void {
    const token = this.tokenGenerado();
    if (token) this.router.navigate(['/recuperar', token]);
  }

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
        const msg = extraerMensajeError(e);
        if (msg.toLowerCase().includes('suspendid') || msg.toLowerCase().includes('desactiv')) {
          this.error.set('Tu cuenta está suspendida o desactivada. Comunícate con la administración de la biblioteca.');
        } else {
          this.error.set(msg);
        }
      },
    });
  }
}