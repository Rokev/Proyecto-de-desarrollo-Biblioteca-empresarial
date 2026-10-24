package persistanceLayer.mapper;

import bussinesLayer.dto.*;
import persistanceLayer.entity.*;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte entre entidades JPA y DTOs.
 * Las relaciones se exponen en el DTO solo como ids (usuarioId, sedeId, ...) y al pasar a entidad
 * se ignoran: las resuelve cada DAO armando referencias por id, igual que en el taller.
 * Los valores por defecto de la entidad (estado, activo, etc.) se aplican aqui con defaultValue,
 * porque MapStruct usa el builder de Lombok y un null del DTO pisaria el valor por defecto.
 * Se usa el nombre completo de la anotacion porque esta interfaz se llama igual (Mapper).
 */
@org.mapstruct.Mapper(componentModel = "spring")
public interface Mapper {

	AutorDTO autorToDto(AutorEntity entity);

	@Mapping(target = "libros", ignore = true)
	AutorEntity autorToEntity(AutorDTO dto);

	@Mapping(target = "prestamoId", source = "prestamo.id")
	@Mapping(target = "sedeDevolucionId", source = "sedeDevolucion.id")
	@Mapping(target = "bibliotecarioId", source = "bibliotecario.id")
	DevolucionDTO devolucionToDto(DevolucionEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "PENDIENTE")
	@Mapping(target = "prestamo", ignore = true)
	@Mapping(target = "sedeDevolucion", ignore = true)
	@Mapping(target = "bibliotecario", ignore = true)
	DevolucionEntity devolucionToEntity(DevolucionDTO dto);

	@Mapping(target = "libroId", source = "libro.id")
	@Mapping(target = "sedeId", source = "sede.id")
	EjemplarDTO ejemplarToDto(EjemplarEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "DISPONIBLE")
	@Mapping(target = "libro", ignore = true)
	@Mapping(target = "sede", ignore = true)
	@Mapping(target = "prestamos", ignore = true)
	EjemplarEntity ejemplarToEntity(EjemplarDTO dto);

	@Mapping(target = "sedeId", source = "sede.id")
	EventoDTO eventoToDto(EventoEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "PROGRAMADO")
	@Mapping(target = "exclusivo", source = "exclusivo", defaultValue = "false")	
	@Mapping(target = "sede", ignore = true)
	@Mapping(target = "inscripciones", ignore = true)
	EventoEntity eventoToEntity(EventoDTO dto);

	GeneroDTO generoToDto(GeneroEntity entity);

	@Mapping(target = "libros", ignore = true)
	GeneroEntity generoToEntity(GeneroDTO dto);

	@Mapping(target = "eventoId", source = "evento.id")
	@Mapping(target = "usuarioId", source = "usuario.id")
	InscripcionEventoDTO inscripcionEventoToDto(InscripcionEventoEntity entity);

	@Mapping(target = "cantidadCupos", source = "cantidadCupos", defaultValue = "1")
	@Mapping(target = "estado", source = "estado", defaultValue = "ACTIVA")
	@Mapping(target = "evento", ignore = true)
	@Mapping(target = "usuario", ignore = true)
	InscripcionEventoEntity inscripcionEventoToEntity(InscripcionEventoDTO dto);


	@Mapping(target = "generoId", source = "genero.id")
	@Mapping(target = "autoresIds", source = "autores")
	LibroDTO libroToDto(LibroEntity entity);

	@Mapping(target = "genero", ignore = true)
	@Mapping(target = "autores", ignore = true)
	@Mapping(target = "ejemplares", ignore = true)
	LibroEntity libroToEntity(LibroDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "tokenPasarela", ignore = true)
	MetodoPagoDTO metodoPagoToDto(MetodoPagoEntity entity);

	@Mapping(target = "predeterminado", source = "predeterminado", defaultValue = "false")
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "pagos", ignore = true)
	MetodoPagoEntity metodoPagoToEntity(MetodoPagoDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "prestamoId", source = "prestamo.id")
	MultaDTO multaToDto(MultaEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "PENDIENTE")
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "prestamo", ignore = true)
	@Mapping(target = "pagos", ignore = true)
	MultaEntity multaToEntity(MultaDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	NotificacionDTO notificacionToDto(NotificacionEntity entity);

	@Mapping(target = "canal", source = "canal", defaultValue = "APP")
	@Mapping(target = "leida", source = "leida", defaultValue = "false")
	@Mapping(target = "usuario", ignore = true)
	NotificacionEntity notificacionToEntity(NotificacionDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "metodoPagoId", source = "metodoPago.id")
	@Mapping(target = "multaId", source = "multa.id")
	@Mapping(target = "suscripcionId", source = "suscripcion.id")
	@Mapping(target = "reservaSalaId", source = "reservaSala.id")
	PagoDTO pagoToDto(PagoEntity entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "metodoPago", ignore = true)
	@Mapping(target = "multa", ignore = true)
	@Mapping(target = "suscripcion", ignore = true)
	@Mapping(target = "reservaSala", ignore = true)
	PagoEntity pagoToEntity(PagoDTO dto);

@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "ejemplarId", source = "ejemplar.id")
	@Mapping(target = "libroId", source = "ejemplar.libro.id")
	@Mapping(target = "libroTitulo", source = "ejemplar.libro.titulo")
	@Mapping(target = "sedeOrigenId", source = "sedeOrigen.id")
	@Mapping(target = "sedeOrigenNombre", source = "sedeOrigen.nombre")
	@Mapping(target = "sedeDevolucionId", source = "sedeDevolucion.id")
	@Mapping(target = "sedeDevolucionNombre", source = "sedeDevolucion.nombre")
	@Mapping(target = "repartidorId", source = "repartidor.id")
	@Mapping(target = "repartidorNombre", source = "repartidor.nombreUsuario")
	@Mapping(target = "prestamoId", source = "prestamo.id")
	@Mapping(target = "bibliotecarioId", source = "bibliotecario.id")
	PedidoInterciudadDTO pedidoInterciudadToDto(PedidoInterciudadEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "SOLICITADO")
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "ejemplar", ignore = true)
	@Mapping(target = "sedeOrigen", ignore = true)
	@Mapping(target = "sedeDevolucion", ignore = true)
	@Mapping(target = "repartidor", ignore = true)
	@Mapping(target = "prestamo", ignore = true)
	@Mapping(target = "bibliotecario", ignore = true)
	PedidoInterciudadEntity pedidoInterciudadToEntity(PedidoInterciudadDTO dto);

	PlanSuscripcionDTO planSuscripcionToDto(PlanSuscripcionEntity entity);

	@Mapping(target = "activo", source = "activo", defaultValue = "true")
	@Mapping(target = "descuentoSalas", source = "descuentoSalas", defaultValue = "0")   
	@Mapping(target = "suscripciones", ignore = true)
	PlanSuscripcionEntity planSuscripcionToEntity(PlanSuscripcionDTO dto);

	PreguntaFrecuenteDTO preguntaFrecuenteToDto(PreguntaFrecuente entity);

	@Mapping(target = "activa", source = "activa", defaultValue = "true")
	PreguntaFrecuente preguntaFrecuenteToEntity(PreguntaFrecuenteDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "ejemplarId", source = "ejemplar.id")
	@Mapping(target = "sedeRetiroId", source = "sedeRetiro.id")
	PrestamoDTO prestamoToDto(PrestamoEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "SOLICITADO")
	@Mapping(target = "modalidad", source = "modalidad", defaultValue = "SEDE")
	@Mapping(target = "vecesRenovado", source = "vecesRenovado", defaultValue = "0")
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "ejemplar", ignore = true)
	@Mapping(target = "sedeRetiro", ignore = true)
	@Mapping(target = "devolucion", ignore = true)
	@Mapping(target = "multas", ignore = true)
	@Mapping(target = "sanciones", ignore = true)
	PrestamoEntity prestamoToEntity(PrestamoDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "libroId", source = "libro.id")
	@Mapping(target = "sedeId", source = "sede.id")
	@Mapping(target = "ejemplarId", source = "ejemplar.id")
	ReservaLibroDTO reservaLibroToDto(ReservaLibroEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "EN_ESPERA")
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "libro", ignore = true)
	@Mapping(target = "sede", ignore = true)
	@Mapping(target = "ejemplar", ignore = true)
	ReservaLibroEntity reservaLibroToEntity(ReservaLibroDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "salaId", source = "sala.id")
	ReservaSalaDTO reservaSalaToDto(ReservaSalaEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "PENDIENTE_PAGO")
	@Mapping(target = "descuento", source = "descuento", defaultValue = "0")
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "sala", ignore = true)
	@Mapping(target = "pagos", ignore = true)
	ReservaSalaEntity reservaSalaToEntity(ReservaSalaDTO dto);

	RolDTO rolToDto(RolEntity entity);

	@Mapping(target = "usuarios", ignore = true)
	RolEntity rolToEntity(RolDTO dto);

	@Mapping(target = "sedeId", source = "sede.id")
	SalaEstudioDTO salaEstudioToDto(SalaEstudioEntity entity);

	@Mapping(target = "activa", source = "activa", defaultValue = "true")
	@Mapping(target = "sede", ignore = true)
	@Mapping(target = "reservas", ignore = true)
	SalaEstudioEntity salaEstudioToEntity(SalaEstudioDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "prestamoId", source = "prestamo.id")
	SancionDTO sancionToDto(SancionEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "ACTIVA")
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "prestamo", ignore = true)
	SancionEntity sancionToEntity(SancionDTO dto);

	SedeDTO sedeToDto(SedeEntity entity);

	@Mapping(target = "aliada", source = "aliada", defaultValue = "false")
	@Mapping(target = "enOperacion", source = "enOperacion", defaultValue = "true")
	@Mapping(target = "personal", ignore = true)
	@Mapping(target = "ejemplares", ignore = true)
	@Mapping(target = "salasEstudio", ignore = true)
	@Mapping(target = "eventos", ignore = true)
	@Mapping(target = "devolucionesRecibidas", ignore = true)
	SedeEntity sedeToEntity(SedeDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	SesionDTO sesionToDto(SesionEntity entity);

	@Mapping(target = "activa", source = "activa", defaultValue = "true")
	@Mapping(target = "usuario", ignore = true)
	SesionEntity sesionToEntity(SesionDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	@Mapping(target = "planId", source = "plan.id")
	SuscripcionDTO suscripcionToDto(SuscripcionEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "ACTIVA")
	@Mapping(target = "renovacionAutomatica", source = "renovacionAutomatica", defaultValue = "true")
	@Mapping(target = "mesesConsecutivos", source = "mesesConsecutivos", defaultValue = "0")
	@Mapping(target = "descuentoSalas", source = "descuentoSalas", defaultValue = "0")   
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "plan", ignore = true)
	@Mapping(target = "pagos", ignore = true)
	SuscripcionEntity suscripcionToEntity(SuscripcionDTO dto);

	TarifaEnvioDTO tarifaEnvioToDto(TarifaEnvioEntity entity);

	TarifaEnvioEntity tarifaEnvioToEntity(TarifaEnvioDTO dto);

	@Mapping(target = "usuarioId", source = "usuario.id")
	TokenRecuperacionDTO tokenRecuperacionToDto(TokenRecuperacionEntity entity);

	@Mapping(target = "usado", source = "usado", defaultValue = "false")
	@Mapping(target = "usuario", ignore = true)
	TokenRecuperacionEntity tokenRecuperacionToEntity(TokenRecuperacionDTO dto);

	@Mapping(target = "ejemplarId", source = "ejemplar.id")
	@Mapping(target = "libroId", source = "ejemplar.libro.id")
	@Mapping(target = "libroTitulo", source = "ejemplar.libro.titulo")
	@Mapping(target = "sedeActualId", source = "sedeActual.id")
	@Mapping(target = "sedeActualNombre", source = "sedeActual.nombre")
	@Mapping(target = "sedeDestinoId", source = "sedeDestino.id")
	@Mapping(target = "sedeDestinoNombre", source = "sedeDestino.nombre")
	@Mapping(target = "devolucionId", source = "devolucion.id")
	@Mapping(target = "bibliotecarioId", source = "bibliotecarioRecepcion.id")
	TrasladoRetornoDTO trasladoRetornoToDto(TrasladoRetornoEntity entity);

	@Mapping(target = "estado", source = "estado", defaultValue = "EN_TRANSITO")
	@Mapping(target = "ejemplar", ignore = true)
	@Mapping(target = "sedeActual", ignore = true)
	@Mapping(target = "sedeDestino", ignore = true)
	@Mapping(target = "devolucion", ignore = true)
	@Mapping(target = "bibliotecarioRecepcion", ignore = true)
	TrasladoRetornoEntity trasladoRetornoToEntity(TrasladoRetornoDTO dto);

	@Mapping(target = "rolId", source = "rol.id")
	@Mapping(target = "sedeId", source = "sede.id")
	@Mapping(target = "contrasena", ignore = true)
	UsuarioDTO usuarioToDto(UsuarioEntity entity);

	@Mapping(target = "activo", source = "activo", defaultValue = "true")
	@Mapping(target = "rol", ignore = true)
	@Mapping(target = "sede", ignore = true)
	@Mapping(target = "prestamos", ignore = true)
	@Mapping(target = "multas", ignore = true)
	@Mapping(target = "suscripciones", ignore = true)
	@Mapping(target = "metodosPago", ignore = true)
	@Mapping(target = "pagos", ignore = true)
	@Mapping(target = "reservasSala", ignore = true)
	@Mapping(target = "inscripcionesEvento", ignore = true)
	@Mapping(target = "reservasLibro", ignore = true)
	@Mapping(target = "sanciones", ignore = true)
	@Mapping(target = "notificaciones", ignore = true)
	@Mapping(target = "sesiones", ignore = true)
	@Mapping(target = "tokensRecuperacion", ignore = true)
	UsuarioEntity usuarioToEntity(UsuarioDTO dto);

	/** Convierte la lista de autores de un libro en la lista de sus ids. */
	default List<Long> autoresToIds(List<AutorEntity> autores) {
		if (autores == null) {
			return new ArrayList<>();
		}
		return autores.stream().map(AutorEntity::getId).toList();
	}
}
