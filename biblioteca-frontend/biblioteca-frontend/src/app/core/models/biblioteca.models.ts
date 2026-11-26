export type RolNombre = 'CLIENTE' | 'BIBLIOTECARIO' | 'REPARTIDOR' | 'ADMIN';

export interface AuthResponseDTO {
  token: string;
  tipo: string;
  expiracion: string;
  usuarioId: number;
  nombre: string;
  rol: RolNombre;
}

export interface LoginRequestDTO {
  correo: string;
  contrasena: string;
}

export interface RegistroDTO {
  nombre: string;
  apellido: string;
  documento: string;
  nombreUsuario: string;
  correo: string;
  contrasena: string;
  telefono?: string;
  direccion?: string;
}

export interface UsuarioDTO {
  id: number;
  nombre: string;
  apellido: string;
  documento: string;
  nombreUsuario: string;
  correo: string;
  contrasena?: string;
  telefono?: string;
  direccion?: string;
  fechaRegistro?: string;
  activo: boolean;
  rolId: number;
  sedeId?: number;
}

export interface SedeDTO {
  id: number;
  nombre: string;
  direccion: string;
  ciudad: string;
  telefono?: string;
  correo?: string;
  latitud?: number;
  longitud?: number;
  horaApertura?: string;
  horaCierre?: string;
  aliada?: boolean;
  enOperacion: boolean;
}

export interface AutorDTO {
  id?: number;
  nombre: string;
  nacionalidad?: string;
  biografia?: string;
}

export interface GeneroDTO {
  id?: number;
  nombre: string;
  descripcion?: string;
}

export interface LibroDTO {
  id: number;
  isbn: string;
  titulo: string;
  idioma: string;
  anioPublicacion: number;
  editorial?: string;
  sinopsis?: string;
  numeroPaginas?: number;
  portadaUrl?: string;
  generoId: number;
  autoresIds: number[];
}

export interface EjemplarDTO {
  id?: number;
  codigoBarras: string;
  estado: 'DISPONIBLE' | 'PRESTADO' | 'RESERVADO' | 'DANADO' | 'PERDIDO' | 'EN_TRANSITO';
  libroId: number;
  sedeId: number;
}

export interface PrestamoDTO {
  id?: number;
  fechaSolicitud?: string;
  fechaEntrega?: string;
  fechaVencimiento?: string;
  fechaDevolucion?: string;
  estado?: string;
  modalidad: 'SEDE' | 'DOMICILIO' | 'INTERCIUDAD';
  direccionEntrega?: string;
  vecesRenovado?: number;
  usuarioId: number;
  ejemplarId: number;
  sedeRetiroId: number;
}

export interface PrestamoDetalleDTO {
  prestamoId: number;
  estado: string;
  modalidad: string;
  usuarioId: number;
  libroId: number;
  libroTitulo: string;
  ejemplarId: number;
  sedeId: number;
  sedeNombre: string;
  fechaSolicitud: string;
  fechaEntrega?: string;
  fechaLimiteDevolucion?: string;
  fechaDevolucion?: string;
}

export interface ReservaLibroDTO {
  id?: number;
  fechaReserva?: string;
  fechaNotificacion?: string;
  fechaLimiteRetiro?: string;
  fechaCancelacion?: string;
  estado?: string;
  usuarioId: number;
  libroId: number;
  sedeId: number;
  ejemplarId?: number;
}

export interface DevolucionDTO {
  id: number;
  codigoQr: string;
  fechaSolicitud: string;
  fechaConfirmacion?: string;
  estado: string;
  observaciones?: string;
  prestamoId: number;
  sedeDevolucionId: number;
  bibliotecarioId?: number;
}

export interface PedidoInterciudadDTO {
  id: number;
  estado: string;
  ciudadEntrega: string;
  direccionEntrega: string;
  fechaRecepcion: string;
  horaRecepcion: string;
  costoEnvio: number;
  fechaSolicitud: string;
  fechaTransito?: string;
  fechaEntrega?: string;
  fechaCancelacion?: string;
  fechaLimiteRecogida?: string;
  fechaLimiteDevolucion?: string;
  usuarioId: number;
  ejemplarId?: number;
  libroId: number;
  libroTitulo: string;
  sedeOrigenId: number;
  sedeOrigenNombre: string;
  sedeDevolucionId: number;
  sedeDevolucionNombre: string;
  repartidorId?: number;
  repartidorNombre?: string;
  prestamoId?: number;
  bibliotecarioId?: number;
}

export interface SolicitudPedidoInterciudadDTO {
  usuarioId: number;
  libroId: number;
  ciudadEntrega: string;
  direccionEntrega: string;
  sedeDevolucionId: number;
  fechaRecepcion: string;
  horaRecepcion: string;
}

export interface SalaEstudioDTO {
  id: number;
  nombre: string;
  capacidad: number;
  descripcion?: string;
  tarifaPorHora: number;
  horaApertura?: string;
  horaCierre?: string;
  activa: boolean;
  sedeId: number;
}

export interface DisponibilidadSalasDTO {
  sedeId: number;
  tieneSalas: boolean;
  totalSalas: number;
  salasDisponibles: number;
  inicio: string;
  fin: string;
  salas: {
    salaId: number;
    nombre: string;
    capacidad: number;
    tarifaPorHora: number;
    disponible: boolean;
  }[];
}

export interface ReservaSalaDTO {
  id?: number;
  fechaInicio: string;
  fechaFin: string;
  estado?: string;
  valorBase?: number;
  descuento?: number;
  valorTotal?: number;
  usuarioId: number;
  salaId: number;
}

export interface EventoDTO {
  id: number;
  titulo: string;
  descripcion: string;
  fechaInicio: string;
  fechaFin: string;
  cupoMaximo: number;
  cuposDisponibles: number;
  estado: string;
  exclusivo: boolean;
  sedeId: number;
}

export interface InscripcionEventoDTO {
  id?: number;
  fechaInscripcion?: string;
  cantidadCupos: number;
  estado?: string;
  eventoId: number;
  usuarioId: number;
}

export interface PlanSuscripcionDTO {
  id: number;
  nombre: string;
  descripcion: string;
  beneficios: string;
  precio: number;
  duracionMeses: number;
  descuentoSalas: number;
  activo: boolean;
}

export interface SuscripcionDTO {
  id: number;
  fechaInicio: string;
  fechaFin: string;
  estado: string;
  valorMensual: number;
  descuentoSalas: number;
  renovacionAutomatica: boolean;
  usuarioId: number;
  planId: number;
}

export interface MultaDTO {
  id: number;
  monto: number;
  motivo: string;
  fechaGeneracion: string;
  estado: string;
  usuarioId: number;
  prestamoId?: number;
}

export interface SancionDTO {
  id: number;
  tipo: string;
  motivo: string;
  fechaAplicacion: string;
  fechaFin?: string;
  estado: string;
  usuarioId: number;
  prestamoId?: number;
}

export interface SolicitudPagoDTO {
  usuarioId: number;
  concepto: 'MULTA' | 'RECARGO' | 'SUSCRIPCION' | 'RESERVA_SALA' | 'ENVIO_INTERCIUDAD';
  monto: number;
  numeroTarjeta: string;
  multaId?: number;
  planSuscripcionId?: number;
  reservaSalaId?: number;
  pedidoInterciudadId?: number;
}

export interface ComprobantePagoDTO {
  id: number;
  numeroReferencia: string;
  estado: 'APROBADO' | 'RECHAZADO' | 'PENDIENTE' | 'REEMBOLSADO';
  concepto: string;
  monto: number;
  ultimosCuatroDigitos: string;
  fechaPago: string;
  mensaje: string;
  usuarioId: number;
}

export interface PagoDTO {
  id: number;
  monto: number;
  fechaPago: string;
  concepto: string;
  estado: string;
  referencia: string;
  usuarioId: number;
}

export interface NotificacionDTO {
  id: number;
  tipo: string;
  titulo: string;
  mensaje: string;
  canal: string;
  fechaCreacion: string;
  leida: boolean;
  usuarioId: number;
}

export interface ChatbotRespuestaDTO {
  respuesta: string;
  preguntaCoincidente?: string;
  categoria?: string;
  coincidencia: number;
  sugerencias: string[];
}

export interface PreguntaFrecuenteDTO {
  id?: number;
  pregunta: string;
  respuesta: string;
  categoria: string;
  activa: boolean;
}

export interface TarifaEnvioDTO {
  id?: number;
  ciudadA: string;
  ciudadB: string;
  tarifa: number;
  fechaActualizacion?: string;
}

export interface AuditoriaDTO {
  id: number;
  fecha: string;
  accion: string;
  modulo: string;
  descripcion: string;
  usuarioId?: number;
  usuarioNombre?: string;
  entidadId?: number;
}

export interface AsistenteEventoDTO {
  inscripcionId: number;
  usuarioId: number;
  nombre: string;
  correo: string;
  cantidadCupos: number;
  estado: string;
  fechaInscripcion: string;
}

export interface ReporteDTO {
  tipo: string;
  titulo: string;
  generadoEn: string;
  desde?: string;
  hasta?: string;
  sedeId?: number;
  columnas: string[];
  filas: string[][];
  resumen: Record<string, string>;
  totalRegistros: number;
}

export interface PoliticaSancionDTO {
  id?: number;
  infraccion: string;
  tipoSancion: 'BLOQUEO_PRESTAMOS' | 'SUSPENSION_CUENTA' | 'ADVERTENCIA';
  duracionDias?: number | null;
  activa: boolean;
  descripcion?: string;
}

export interface PoliticaMultaDTO {
  id?: number;
  concepto: string;
  monto: number;
  descripcion?: string;
}

export interface PlazoSistemaDTO {
  id?: number;
  clave: string;
  valor: number;
  unidad?: string;
  descripcion?: string;
}
