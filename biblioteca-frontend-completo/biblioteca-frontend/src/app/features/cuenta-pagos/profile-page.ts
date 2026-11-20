import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesUiService } from '../../core/services/notificaciones-ui.service';
import { extraerMensajeError } from '../../core/utils/errores.util';
import { Button } from '../../shared/ui/atoms/button/button';
import { Card } from '../../shared/ui/atoms/card/card';
import { FormField } from '../../shared/ui/molecules/form-field/form-field';
import { AccountTemplate } from '../../shared/ui/templates/account-template/account-template';

@Component({
  selector: 'app-profile-page',
  imports: [FormsModule, AccountTemplate, Card, FormField, Button],
  template: `
    <app-account-template titulo="Mi Perfil">
      <app-card>
        <form (ngSubmit)="guardar()">
          <app-form-field etiqueta="Nombre" para="pNom"><input id="pNom" class="campo" [(ngModel)]="nombre" name="nom" /></app-form-field>
          <app-form-field etiqueta="Apellido" para="pApe"><input id="pApe" class="campo" [(ngModel)]="apellido" name="ape" /></app-form-field>
          <app-form-field etiqueta="Teléfono" para="pTel"><input id="pTel" class="campo" [(ngModel)]="telefono" name="tel" /></app-form-field>
          <app-form-field etiqueta="Dirección" para="pDir"><input id="pDir" class="campo" [(ngModel)]="direccion" name="dir" /></app-form-field>
          <app-button tipo="submit" texto="Actualizar Datos" />
        </form>
      </app-card>
    </app-account-template>
  `
})
export class ProfilePage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly ui = inject(NotificacionesUiService);

  protected nombre = '';
  protected apellido = '';
  protected telefono = '';
  protected direccion = '';

  ngOnInit(): void {
    const p = this.auth.perfil();
    if (p) {
      this.nombre = p.nombre; this.apellido = p.apellido;
      this.telefono = p.telefono ?? ''; this.direccion = p.direccion ?? '';
    }
  }

  protected guardar(): void {
    this.auth.actualizarPerfil({
      nombre: this.nombre, apellido: this.apellido,
      telefono: this.telefono, direccion: this.direccion,
    }).subscribe({
      next: () => this.ui.mostrar('Perfil actualizado.', 'exito'),
      error: (e) => this.ui.mostrar(extraerMensajeError(e), 'error'),
    });
  }
}
