export interface Generador {
  generatorId?: number;
  name: string;
  email: string;
  contact: string; // Cambiado de number a string para mayor flexibilidad
  address: string;
  type: string;
  state?: string;
  zona?: string;
  acceptTerms?: boolean;
  latitude?: number;
  longitude?: number;
}
