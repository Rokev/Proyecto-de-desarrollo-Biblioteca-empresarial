import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'truncar' })
export class TruncarPipe implements PipeTransform {
  transform(texto: string | null | undefined, max = 90): string {
    if (!texto) return '';
    return texto.length <= max ? texto : texto.slice(0, max).trimEnd() + '...';
  }
}
