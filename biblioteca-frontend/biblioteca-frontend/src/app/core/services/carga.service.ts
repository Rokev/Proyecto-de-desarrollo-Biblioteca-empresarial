import { computed, Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class CargaService {
  private readonly contador = signal(0);
  readonly activa = computed(() => this.contador() > 0);
  iniciar(): void { this.contador.update((n) => n + 1); }
  terminar(): void { this.contador.update((n) => Math.max(0, n - 1)); }
}
