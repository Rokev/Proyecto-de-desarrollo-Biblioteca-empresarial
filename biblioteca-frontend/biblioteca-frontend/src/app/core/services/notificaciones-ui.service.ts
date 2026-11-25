import { Injectable, signal } from '@angular/core';

export type TipoToast = 'exito' | 'error' | 'advertencia' | 'info';
export interface ToastItem { id: number; tipo: TipoToast; texto: string; }

@Injectable({ providedIn: 'root' })
export class NotificacionesUiService {
  private seq = 1;
  private readonly items = signal<ToastItem[]>([]);
  readonly toasts = this.items.asReadonly();

  mostrar(texto: string, tipo: TipoToast = 'info', ms = 5000): void {
    if (this.items().some((t) => t.texto === texto)) return;
    const id = this.seq++;
    this.items.update((prev) => [...prev, { id, tipo, texto }]);
    setTimeout(() => this.cerrar(id), ms);
  }

  cerrar(id: number): void {
    this.items.update((prev) => prev.filter((t) => t.id !== id));
  }
}
