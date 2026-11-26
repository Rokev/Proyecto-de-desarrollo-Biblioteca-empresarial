import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Alert } from '../../shared/ui/molecules/alert/alert';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { AuthTemplate } from '../../shared/ui/templates/auth-template/auth-template';

@Component({
  selector: 'app-recover-password-page',
  imports: [FormsModule, RouterLink, AuthTemplate, FormField, Button, Alert],
  template: `
    <app-auth-template>
      <h1>Recuperar contraseña</h1>
      @if (error()) { <app-alert titulo="Atención" [mensaje]="error()!" tipo="error" /> }
      @if (mensajeOk()) { <app-alert titulo="Solicitud enviada" [mensaje]="mensajeOk()!" tipo="exito" /> }
      @if (tokenGenerado()) {
        <p>Simulamos el envío al correo. Tu token de recuperación (válido 24 horas, un solo uso) es:</p>
        <p><code>{{ tokenGenerado() }}</code></p>
        <app-button texto="Continuar y cambiar contraseña" (presionado)="continuar()" />
      }

      @if (!token() && !tokenGenerado()) {
        <form (ngSubmit)="solicitar()">
          <app-form-field etiqueta="Correo registrado" para="recCorreo" [obligatorio]="true" ayuda="El token expira en 24 horas.">
            <input id="recCorreo" type="email" class="campo" [(ngModel)]="correo" name="correo" required />
          </app-form-field>
          <app-button tipo="submit" texto="Enviar token de recuperación" [cargando]="cargando()" />
        </form>
      } @else if (token()) {
        <form (ngSubmit)="restablecer()">
          <app-form-field etiqueta="Nueva contraseña (mín. 8 caracteres)" para="newPass" [obligatorio]="true">
            <input id="newPass" type="password" class="campo" [(ngModel)]="nuevaContrasena" name="pass" required minlength="8" />
          </app-form-field>
          <app-button tipo="submit" texto="Guardar nueva contraseña" [cargando]="cargando()" />
        </form>
      }
      <p class="pie"><a routerLink="/login">Volver a iniciar sesión</a></p>
    </app-auth-template>
  `,
  styles: [`.pie { margin-top: 1rem; a { color: var(--color-primario); } }`]
})
export class RecoverPasswordPage {
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);
  private readonly router = inject(Router);

  readonly token = input<string>();
  protected correo = '';
  protected nuevaContrasena = '';
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly mensajeOk = signal<string | null>(null);
  protected readonly tokenGenerado = signal<string | null>(null);

  protected continuar(): void {
    const t = this.tokenGenerado();
    if (t) this.router.navigate(['/recuperar', t]);
  }

  protected solicitar(): void {
    if (!this.correo.trim()) return;
    this.cargando.set(true);
    this.error.set(null);
    this.auth.solicitarRecuperacion(this.correo.trim()).subscribe({
      next: (r) => {
        this.cargando.set(false);
        if (r.token) { this.tokenGenerado.set(r.token); } else { this.mensajeOk.set(r.mensaje); }
      },
      error: (e) => { this.cargando.set(false); this.error.set(extraerMensajeError(e)); },
    });
  }

  protected restablecer(): void {
    if (this.nuevaContrasena.length < 8 || !this.token()) {
      this.error.set('La contraseña debe tener al menos 8 caracteres.');
      return;
    }
    this.cargando.set(true);
    this.auth.restablecerContrasena(this.token()!, this.nuevaContrasena).subscribe({
      next: () => {
        this.cargando.set(false);
        this.ui.mostrar('Contraseña restablecida correctamente.', 'exito');
        this.router.navigate(['/login']);
      },
      error: (e) => { this.cargando.set(false); this.error.set(extraerMensajeError(e)); },
    });
  }
}