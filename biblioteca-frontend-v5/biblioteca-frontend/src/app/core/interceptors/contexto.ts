import { HttpContextToken } from '@angular/common/http';
export const SIN_INDICADOR_CARGA = new HttpContextToken<boolean>(() => false);
export const SIN_NOTIFICACION_ERROR = new HttpContextToken<boolean>(() => false);
