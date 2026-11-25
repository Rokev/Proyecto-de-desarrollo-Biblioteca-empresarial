import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AutorDTO, EjemplarDTO, GeneroDTO, LibroDTO, SedeDTO } from '../models/biblioteca.models';

export interface FiltrosLibro {
  titulo?: string;
  autor?: string;
  idioma?: string;
  anio?: number | null;
  generoId?: number | null;
  disponible?: boolean | null;
  sedeId?: number | null;
}

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  buscarLibros(filtros: FiltrosLibro = {}): Observable<LibroDTO[]> {
    let params = new HttpParams();
    if (filtros.titulo) params = params.set('titulo', filtros.titulo);
    if (filtros.autor) params = params.set('autor', filtros.autor);
    if (filtros.idioma) params = params.set('idioma', filtros.idioma);
    if (filtros.anio) params = params.set('anio', filtros.anio);
    if (filtros.generoId) params = params.set('generoId', filtros.generoId);
    if (filtros.disponible !== null && filtros.disponible !== undefined) {
      params = params.set('disponible', filtros.disponible);
    }
    if (filtros.sedeId) params = params.set('sedeId', filtros.sedeId);
    return this.http.get<LibroDTO[]>(`${this.api}/libros/buscar`, { params });
  }

  obtenerLibro(id: number): Observable<LibroDTO> {
    return this.http.get<LibroDTO>(`${this.api}/libros/${id}`);
  }

  crearLibro(dto: Partial<LibroDTO>): Observable<LibroDTO> {
    return this.http.post<LibroDTO>(`${this.api}/libros`, dto);
  }

  actualizarLibro(id: number, dto: Partial<LibroDTO>): Observable<LibroDTO> {
    return this.http.put<LibroDTO>(`${this.api}/libros/${id}`, dto);
  }

  eliminarLibro(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/libros/${id}`);
  }

  // Ejemplares y Stock
  listarEjemplares(): Observable<EjemplarDTO[]> {
    return this.http.get<EjemplarDTO[]>(`${this.api}/ejemplares`);
  }

  crearEjemplar(dto: EjemplarDTO): Observable<EjemplarDTO> {
    return this.http.post<EjemplarDTO>(`${this.api}/ejemplares`, dto);
  }

  actualizarEjemplar(id: number, dto: EjemplarDTO): Observable<EjemplarDTO> {
    return this.http.put<EjemplarDTO>(`${this.api}/ejemplares/${id}`, dto);
  }

  eliminarEjemplar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/ejemplares/${id}`);
  }

  // Autores y Géneros
  listarAutores(): Observable<AutorDTO[]> {
    return this.http.get<AutorDTO[]>(`${this.api}/autores`);
  }

  crearAutor(dto: AutorDTO): Observable<AutorDTO> {
    return this.http.post<AutorDTO>(`${this.api}/autores`, dto);
  }

  listarGeneros(): Observable<GeneroDTO[]> {
    return this.http.get<GeneroDTO[]>(`${this.api}/generos`);
  }

  crearGenero(dto: GeneroDTO): Observable<GeneroDTO> {
    return this.http.post<GeneroDTO>(`${this.api}/generos`, dto);
  }

  // Sedes
  listarSedesActivas(): Observable<SedeDTO[]> {
    return this.http.get<SedeDTO[]>(`${this.api}/sedes/activas`);
  }

  listarTodasSedes(): Observable<SedeDTO[]> {
    return this.http.get<SedeDTO[]>(`${this.api}/sedes`);
  }

  crearSede(dto: Partial<SedeDTO>): Observable<SedeDTO> {
    return this.http.post<SedeDTO>(`${this.api}/sedes`, dto);
  }

  actualizarSede(id: number, dto: Partial<SedeDTO>): Observable<SedeDTO> {
    return this.http.put<SedeDTO>(`${this.api}/sedes/${id}`, dto);
  }

  desactivarSede(id: number): Observable<SedeDTO> {
    return this.http.post<SedeDTO>(`${this.api}/sedes/${id}/desactivar`, {});
  }

  reactivarSede(id: number): Observable<SedeDTO> {
    return this.http.post<SedeDTO>(`${this.api}/sedes/${id}/reactivar`, {});
  }
}
