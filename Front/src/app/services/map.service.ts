import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of, throwError } from 'rxjs';
import { catchError, timeout, retry, map } from 'rxjs/operators';
import * as L from 'leaflet';
import { Icon } from "leaflet";

// Interfaces para mapas
export interface MapLocation {
  latitude: number;
  longitude: number;
  accuracy?: number;
}

export interface RouteData {
  coordinates: [number, number][];
  distance: number;
  duration: number;
  instructions?: string[];
}

export interface MapMarker {
  id: string;
  location: MapLocation;
  title: string;
  description?: string;
  type: 'employee' | 'generator' | 'depot';
  status?: 'PENDIENTE' | 'COMPLETADO' | 'EN_PROCESO';
}

// 🆕 Tipos de error más específicos
export enum LocationErrorType {
  PERMISSION_DENIED = 1,
  POSITION_UNAVAILABLE = 2,
  TIMEOUT = 3,
  NOT_SUPPORTED = 4,
  UNKNOWN = 5
}

export interface LocationError {
  type: LocationErrorType;
  message: string;
  canRetry: boolean;
  suggestedAction: string;
}

@Injectable({
  providedIn: 'root'
})
export class MapService {
  private readonly apiUrl = 'https://eu1.locationiq.com/v1';
  private readonly apiKey = 'pk.352f5f6d506b7a660b0ae4babb04698d'; // Tu API key

  // Ubicación actual del empleado
  private currentLocationSubject = new BehaviorSubject<MapLocation | null>(null);
  public currentLocation$ = this.currentLocationSubject.asObservable();

  // 🆕 Estado de disponibilidad de geolocalización
  private isGeolocationAvailable = false;
  private geolocationChecked = false;

  // 🆕 Ubicación de respaldo (Córdoba, Argentina como ejemplo)
  private readonly fallbackLocation: MapLocation = {
    latitude: -31.4201,
    longitude: -64.1888,
    accuracy: 0
  };

  constructor(private http: HttpClient) {
    this.checkGeolocationSupport();
  }

  // ===================================================
  // 🆕 VERIFICACIÓN DE SOPORTE DE GEOLOCALIZACIÓN
  // ===================================================

  /**
   * Verificar si la geolocalización está soportada y disponible
   */
  private checkGeolocationSupport(): void {
    this.isGeolocationAvailable = 'geolocation' in navigator &&
      'getCurrentPosition' in navigator.geolocation;
    this.geolocationChecked = true;

    if (!this.isGeolocationAvailable) {
      console.warn('Geolocalización no soportada en este navegador');
    }
  }

  /**
   * Verificar si la geolocalización está disponible
   */
  public isGeolocationSupported(): boolean {
    if (!this.geolocationChecked) {
      this.checkGeolocationSupport();
    }
    return this.isGeolocationAvailable;
  }

  // ===================================================
  // 🆕 GESTIÓN DE UBICACIÓN MEJORADA
  // ===================================================

  /**
   * Obtener ubicación actual del dispositivo con manejo de errores mejorado
   */
  getCurrentLocation(useHighAccuracy: boolean = true): Observable<MapLocation> {
    return new Observable<MapLocation>(observer => {
      // Verificar soporte básico
      if (!this.isGeolocationSupported()) {
        const error: LocationError = {
          type: LocationErrorType.NOT_SUPPORTED,
          message: 'Tu navegador no soporta geolocalización',
          canRetry: false,
          suggestedAction: 'Actualiza tu navegador o usa uno diferente'
        };
        observer.error(error);
        return;
      }

      // Opciones de geolocalización más permisivas
      const options: PositionOptions = {
        enableHighAccuracy: useHighAccuracy,
        timeout: 15000, // 15 segundos
        maximumAge: 300000 // 5 minutos
      };

      // Función para manejar éxito
      const onSuccess = (position: GeolocationPosition) => {
        const location: MapLocation = {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          accuracy: position.coords.accuracy
        };

        console.log('✅ Ubicación obtenida:', location);
        this.currentLocationSubject.next(location);
        observer.next(location);
        observer.complete();
      };

      // Función para manejar errores
      const onError = (error: GeolocationPositionError) => {
        console.error('❌ Error de geolocalización:', error);

        const locationError = this.parseGeolocationError(error);

        // Intentar con menor precisión si falló con alta precisión
        if (useHighAccuracy && error.code === 2) {
          console.log('🔄 Reintentando con menor precisión...');
          this.getCurrentLocation(false).subscribe({
            next: (location) => observer.next(location),
            error: (err) => observer.error(locationError),
            complete: () => observer.complete()
          });
          return;
        }

        observer.error(locationError);
      };

      // Ejecutar geolocalización
      try {
        navigator.geolocation.getCurrentPosition(onSuccess, onError, options);
      } catch (error) {
        console.error('Error ejecutando getCurrentPosition:', error);
        observer.error({
          type: LocationErrorType.UNKNOWN,
          message: 'Error inesperado al acceder a la geolocalización',
          canRetry: true,
          suggestedAction: 'Intenta nuevamente'
        });
      }
    }).pipe(
      timeout(20000), // Timeout global de 20 segundos
      catchError((error) => {
        console.error('Error final en getCurrentLocation:', error);
        return this.handleLocationError(error);
      })
    );
  }

  /**
   * Obtener ubicación con respaldo automático
   */
  getCurrentLocationWithFallback(): Observable<MapLocation> {
    return new Observable<MapLocation>(observer => {
      this.getCurrentLocation().subscribe({
        next: (location) => {
          observer.next(location);
          observer.complete();
        },
        error: (error) => {
          console.warn('⚠️ Usando ubicación de respaldo:', error);

          // Usar ubicación de respaldo
          this.currentLocationSubject.next(this.fallbackLocation);
          observer.next(this.fallbackLocation);
          observer.complete();
        }
      });
    });
  }

  /**
   * Iniciar seguimiento de ubicación en tiempo real mejorado
   */
  startLocationTracking(useHighAccuracy: boolean = false): Observable<MapLocation> {
    return new Observable<MapLocation>(observer => {
      if (!this.isGeolocationSupported()) {
        observer.error({
          type: LocationErrorType.NOT_SUPPORTED,
          message: 'Geolocalización no soportada',
          canRetry: false,
          suggestedAction: 'Usar ubicación manual'
        });
        return;
      }

      // Opciones menos agresivas para tracking
      const options: PositionOptions = {
        enableHighAccuracy: useHighAccuracy,
        timeout: 10000, // 10 segundos
        maximumAge: 60000 // 1 minuto
      };

      const onSuccess = (position: GeolocationPosition) => {
        const location: MapLocation = {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          accuracy: position.coords.accuracy
        };

        this.currentLocationSubject.next(location);
        observer.next(location);
      };

      const onError = (error: GeolocationPositionError) => {
        console.warn('⚠️ Error en tracking de ubicación:', error);
        const locationError = this.parseGeolocationError(error);
        observer.error(locationError);
      };

      let watchId: number;

      try {
        watchId = navigator.geolocation.watchPosition(onSuccess, onError, options);
      } catch (error) {
        observer.error({
          type: LocationErrorType.UNKNOWN,
          message: 'Error iniciando seguimiento de ubicación',
          canRetry: true,
          suggestedAction: 'Intenta nuevamente'
        });
        return;
      }

      // Cleanup function
      return () => {
        if (watchId) {
          navigator.geolocation.clearWatch(watchId);
        }
      };
    });
  }

  // ===================================================
  // 🆕 MANEJO DE ERRORES MEJORADO
  // ===================================================

  /**
   * Parsear errores de geolocalización a formato más útil
   */
  private parseGeolocationError(error: GeolocationPositionError): LocationError {
    switch (error.code) {
      case 1: // PERMISSION_DENIED
        return {
          type: LocationErrorType.PERMISSION_DENIED,
          message: 'Permisos de ubicación denegados',
          canRetry: true,
          suggestedAction: 'Habilita los permisos de ubicación en tu navegador'
        };

      case 2: // POSITION_UNAVAILABLE
        return {
          type: LocationErrorType.POSITION_UNAVAILABLE,
          message: 'No se puede determinar tu ubicación',
          canRetry: true,
          suggestedAction: 'Verifica tu conexión a internet o intenta en exterior'
        };

      case 3: // TIMEOUT
        return {
          type: LocationErrorType.TIMEOUT,
          message: 'Tiempo agotado obteniendo ubicación',
          canRetry: true,
          suggestedAction: 'Intenta nuevamente'
        };

      default:
        return {
          type: LocationErrorType.UNKNOWN,
          message: error.message || 'Error desconocido de geolocalización',
          canRetry: true,
          suggestedAction: 'Intenta nuevamente o usa ubicación manual'
        };
    }
  }

  /**
   * Manejar errores y ofrecer alternativas
   */
  private handleLocationError(error: any): Observable<MapLocation> {
    console.error('🔥 Manejo final de error:', error);

    // Si es un error de timeout, usar ubicación de respaldo
    if (error.name === 'TimeoutError') {
      console.log('⏰ Timeout - usando ubicación de respaldo');
      return of(this.fallbackLocation);
    }

    // Para otros errores, propagar el error
    return throwError(() => error);
  }

  // ===================================================
  // 🆕 MÉTODOS DE UTILIDAD PARA PERMISOS
  // ===================================================

  /**
   * Verificar estado de permisos de geolocalización
   */
  async checkLocationPermission(): Promise<'granted' | 'denied' | 'prompt' | 'unsupported'> {
    if (!this.isGeolocationSupported()) {
      return 'unsupported';
    }

    if ('permissions' in navigator) {
      try {
        const permission = await navigator.permissions.query({ name: 'geolocation' });
        return permission.state;
      } catch (error) {
        console.warn('No se pueden verificar permisos:', error);
      }
    }

    return 'prompt'; // Valor por defecto
  }

  /**
   * Solicitar permisos de ubicación explícitamente
   */
  requestLocationPermission(): Observable<boolean> {
    return new Observable<boolean>(observer => {
      if (!this.isGeolocationSupported()) {
        observer.next(false);
        observer.complete();
        return;
      }

      // Hacer una llamada simple para activar el prompt de permisos
      navigator.geolocation.getCurrentPosition(
        () => {
          observer.next(true);
          observer.complete();
        },
        (error) => {
          observer.next(error.code !== 1); // true si NO es permission denied
          observer.complete();
        },
        { timeout: 5000, maximumAge: 300000 }
      );
    });
  }

  // ===================================================
  // RUTAS Y DIRECCIONES (sin cambios)
  // ===================================================

  /**
   * Calcular ruta entre dos puntos usando LocationIQ
   */
  calculateRoute(origin: MapLocation, destination: MapLocation): Observable<RouteData> {
    const url = `${this.apiUrl}/directions/driving/${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}`;

    const params = {
      key: this.apiKey,
      steps: 'true',
      geometries: 'geojson',
      overview: 'full'
    };

    return new Observable<RouteData>(observer => {
      this.http.get<any>(url, { params }).subscribe({
        next: (response) => {
          if (response.routes && response.routes.length > 0) {
            const route = response.routes[0];
            const geometry = route.geometry;

            const routeData: RouteData = {
              coordinates: geometry.coordinates.map((coord: number[]) => [coord[1], coord[0]]), // Leaflet usa [lat, lng]
              distance: route.distance / 1000, // Convertir a kilómetros
              duration: route.duration / 60, // Convertir a minutos
              instructions: route.legs[0]?.steps?.map((step: any) => step.maneuver.instruction) || []
            };

            observer.next(routeData);
            observer.complete();
          } else {
            observer.error('No se pudo calcular la ruta');
          }
        },
        error: (error) => {
          console.error('Error calculating route:', error);
          // Fallback: línea recta
          const straightLineRoute: RouteData = {
            coordinates: [
              [origin.latitude, origin.longitude],
              [destination.latitude, destination.longitude]
            ],
            distance: this.calculateDistance(origin.latitude, origin.longitude, destination.latitude, destination.longitude),
            duration: 0
          };
          observer.next(straightLineRoute);
          observer.complete();
        }
      });
    });
  }

  // ===================================================
  // UTILIDADES PARA MAPAS (sin cambios)
  // ===================================================

  /**
   * Calcular distancia entre dos puntos (Haversine)
   */
  calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371; // Radio de la Tierra en km
    const dLat = this.toRadians(lat2 - lat1);
    const dLon = this.toRadians(lon2 - lon1);

    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRadians(lat1)) * Math.cos(this.toRadians(lat2)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c;
  }

  private toRadians(degrees: number): number {
    return degrees * (Math.PI / 180);
  }

  /**
   * Formatear distancia para mostrar
   */
  formatDistance(distanceKm: number): string {
    if (distanceKm < 1) {
      return `${Math.round(distanceKm * 1000)} m`;
    } else {
      return `${distanceKm.toFixed(1)} km`;
    }
  }

  /**
   * Formatear tiempo en minutos
   */
  formatTime(minutes: number): string {
    if (minutes < 1) {
      return '< 1 min';
    } else if (minutes < 60) {
      return `${Math.round(minutes)} min`;
    } else {
      const hours = Math.floor(minutes / 60);
      const remainingMinutes = Math.round(minutes % 60);
      return `${hours}h ${remainingMinutes}min`;
    }
  }

  // ===================================================
  // CONFIGURACIÓN DE ICONOS LEAFLET (sin cambios)
  // ===================================================

  /**
   * Configurar iconos personalizados para marcadores
   */
  static configureLeafletIcons(): void {
    // Configurar iconos por defecto de Leaflet
    const iconRetinaUrl = 'assets/marker-icon-2x.png';
    const iconUrl = 'assets/marker-icon.png';
    const shadowUrl = 'assets/marker-shadow.png';

    const iconDefault = L.icon({
      iconRetinaUrl,
      iconUrl,
      shadowUrl,
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      tooltipAnchor: [16, -28],
      shadowSize: [41, 41]
    });

    L.Marker.prototype.options.icon = iconDefault;
  }

  /**
   * Crear icono personalizado para diferentes tipos de marcadores
   */
  createCustomIcon(type: 'employee' | 'generator' | 'depot', status?: string): L.Icon {
    let color = '#1565c0'; // Azul por defecto
    let icon = '📍';

    switch (type) {
      case 'employee':
        color = '#4caf50';
        icon = '👤';
        break;
      case 'generator':
        switch (status) {
          case 'PENDIENTE':
            color = '#f44336';
            icon = '🏢';
            break;
          case 'EN_PROCESO':
            color = '#ff9800';
            icon = '🏢';
            break;
          case 'COMPLETADO':
            color = '#4caf50';
            icon = '✅';
            break;
          default:
            color = '#9e9e9e';
            icon = '🏢';
        }
        break;
      case 'depot':
        color = '#1565c0';
        icon = '🏪';
        break;
    }

    return <Icon>L.divIcon({
      html: `
        <div style="
          background-color: ${color};
          border: 3px solid white;
          border-radius: 50%;
          width: 30px;
          height: 30px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 16px;
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
        ">
          ${icon}
        </div>
      `,
      className: 'custom-marker',
      iconSize: [30, 30],
      iconAnchor: [15, 15],
      popupAnchor: [0, -15]
    });
  }

  /**
   * Obtener ubicación actual almacenada
   */
  getCurrentLocationValue(): MapLocation | null {
    return this.currentLocationSubject.value;
  }

  /**
   * Establecer ubicación de respaldo personalizada
   */
  setFallbackLocation(location: MapLocation): void {
    Object.assign(this.fallbackLocation, location);
  }

  /**
   * Obtener ubicación de respaldo
   */
  getFallbackLocation(): MapLocation {
    return { ...this.fallbackLocation };
  }
}
