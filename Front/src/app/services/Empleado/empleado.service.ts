import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import {
  EmployeeRoadmapResponse,
  RoadmapCloseRequest,
  RoadmapCloseResponse,
  ScanQRResponse
} from '../../models/Interfaces';

// Interfaces para geolocalización
interface OptimizedRouteResponse {
  roadmapId: number;
  zone: string;
  waypoints: RouteWaypoint[];
  summary: RouteSummary;
  status: string;
}

interface RouteWaypoint {
  order: number;
  generatorId: number;
  generatorName: string;
  address: string;
  latitude: number;
  longitude: number;
  estimatedBags: number;
  estimatedTimeMinutes: number;
  distanceFromPrevious: number;
  arrivalTime: string;
  status: string;
}

interface RouteSummary {
  totalDistance: number;
  totalTimeMinutes: number;
  totalGenerators: number;
  totalBags: number;
  startTime: string;
  estimatedEndTime: string;
  optimizationMethod: string;
}

@Injectable({
  providedIn: 'root'
})
export class EmpleadoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8081/employee';
  private readonly routeUrl = 'http://localhost:8081/api/routes';

  // ===================================================
  // MÉTODOS EXISTENTES
  // ===================================================

  getCurrentEmployeeInfo(): Observable<any> {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      return of({ error: 'No user ID found' });
    }
    return this.http.get(`${this.baseUrl}/by-user/${userId}`);
  }

  getRoadmaps(): Observable<EmployeeRoadmapResponse[]> {
    return new Observable(observer => {
      this.getCurrentEmployeeInfo().subscribe({
        next: (employeeInfo) => {
          if (employeeInfo.error) {
            observer.error(employeeInfo.error);
            return;
          }

          this.http.get<EmployeeRoadmapResponse[]>(`${this.baseUrl}/${employeeInfo.employeeId}/roadmaps`)
            .subscribe({
              next: (roadmaps) => {
                observer.next(roadmaps);
                observer.complete();
              },
              error: (error) => {
                observer.error(error);
              }
            });
        },
        error: (error) => {
          observer.error(error);
        }
      });
    });
  }

  getSpecificRoadmap(roadmapId: number): Observable<EmployeeRoadmapResponse> {
    return new Observable(observer => {
      this.getCurrentEmployeeInfo().subscribe({
        next: (employeeInfo) => {
          if (employeeInfo.error) {
            observer.error(employeeInfo.error);
            return;
          }

          this.http.get<EmployeeRoadmapResponse>(`${this.baseUrl}/${employeeInfo.employeeId}/roadmap/${roadmapId}`)
            .subscribe({
              next: (roadmap) => {
                observer.next(roadmap);
                observer.complete();
              },
              error: (error) => {
                observer.error(error);
              }
            });
        },
        error: (error) => {
          observer.error(error);
        }
      });
    });
  }

  scanQR(qrContent: string, roadmapId: number): Observable<ScanQRResponse> {
    return new Observable(observer => {
      this.getCurrentEmployeeInfo().subscribe({
        next: (employeeInfo) => {
          if (employeeInfo.error) {
            observer.error(employeeInfo.error);
            return;
          }

          const request = {
            qrContent: qrContent,
            roadmapId: roadmapId,
            employeeId: employeeInfo.employeeId
          };

          this.http.post<ScanQRResponse>(`${this.baseUrl}/scan-qr`, request)
            .subscribe({
              next: (response) => {
                observer.next(response);
                observer.complete();
              },
              error: (error) => {
                observer.error(error);
              }
            });
        },
        error: (error) => {
          observer.error(error);
        }
      });
    });
  }

  // ===================================================
  // NUEVOS MÉTODOS PARA RUTAS OPTIMIZADAS
  // ===================================================

  /**
   * Obtener ruta optimizada para una hoja de ruta específica
   */
  getOptimizedRoute(roadmapId: number): Observable<OptimizedRouteResponse> {
    return this.http.get<OptimizedRouteResponse>(`${this.routeUrl}/${roadmapId}/optimize`)
      .pipe(
        catchError(error => {
          console.error('Error getting optimized route:', error);
          throw error;
        })
      );
  }

  /**
   * Verificar si una hoja de ruta tiene coordenadas válidas para optimización
   */
  canOptimizeRoute(roadmapId: number): Observable<boolean> {
    return this.getOptimizedRoute(roadmapId).pipe(
      map(route => {
        // Si tiene waypoints válidos, se puede optimizar
        const hasValidWaypoints = route.waypoints && route.waypoints.length > 0 &&
          route.waypoints.every(wp => wp.latitude && wp.longitude);
        return hasValidWaypoints;
      }),
      catchError(() => of(false)) // Si hay error, no se puede optimizar
    );
  }

  /**
   * Actualizar ubicación del empleado en tiempo real
   */
  updateEmployeeLocation(latitude: number, longitude: number, status: string): Observable<any> {
    return new Observable(observer => {
      this.getCurrentEmployeeInfo().subscribe({
        next: (employeeInfo) => {
          if (employeeInfo.error) {
            observer.error(employeeInfo.error);
            return;
          }

          const locationUpdate = {
            employeeId: employeeInfo.employeeId,
            latitude: latitude,
            longitude: longitude,
            timestamp: new Date().toISOString(),
            status: status
          };

          // Por ahora solo log, puedes implementar endpoint si necesitas tracking
          console.log('📍 Ubicación actualizada:', locationUpdate);
          observer.next({ success: true });
          observer.complete();
        },
        error: (error) => {
          observer.error(error);
        }
      });
    });
  }

  /**
   * Obtener distancia y tiempo estimado entre dos puntos
   */
  getDistanceAndTime(fromLat: number, fromLng: number, toLat: number, toLng: number): Observable<any> {
    // Cálculo básico usando fórmula Haversine
    const distance = this.calculateDistance(fromLat, fromLng, toLat, toLng);
    const timeMinutes = Math.ceil((distance / 40) * 60); // Asumiendo 40 km/h promedio

    return of({
      distance: distance,
      timeMinutes: timeMinutes,
      formattedDistance: this.formatDistance(distance),
      formattedTime: this.formatTime(timeMinutes)
    });
  }

  // ===================================================
  // MÉTODOS DE GEOLOCALIZACIÓN Y NAVEGACIÓN
  // ===================================================

  /**
   * Obtener ubicación actual del dispositivo
   */
  ggetCurrentLocation(): Observable<{latitude: number, longitude: number}> {
    return new Observable(observer => {
      if (!navigator.geolocation) {
        observer.error('Geolocalización no soportada');
        return; // 🔧 IMPORTANTE: return aquí
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          observer.next({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          });
          observer.complete(); // 🔧 IMPORTANTE: completar el observable
        },
        (error) => {
          console.error('Error obteniendo ubicación:', error);
          observer.error('No se pudo obtener la ubicación');
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 300000 // 5 minutos
        }
      );

      // 🔧 IMPORTANTE: return undefined (opcional pero explícito)
      return undefined;
    });
  }

  watchLocation(): Observable<{latitude: number, longitude: number}> {
    return new Observable(observer => {
      if (!navigator.geolocation) {
        observer.error('Geolocalización no soportada');
        return; // 🔧 IMPORTANTE: return aquí para evitar continuar
      }

      const watchId = navigator.geolocation.watchPosition(
        (position) => {
          observer.next({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          });
        },
        (error) => {
          console.error('Error watching location:', error);
          observer.error(error);
        },
        {
          enableHighAccuracy: true,
          timeout: 15000,
          maximumAge: 60000 // 1 minuto
        }
      );

      // 🔧 IMPORTANTE: Función de cleanup que siempre se retorna
      return () => {
        if (navigator.geolocation && watchId !== undefined) {
          navigator.geolocation.clearWatch(watchId);
        }
      };
    });
  }

  /**
   * Navegar a un generador usando app externa
   */
  navigateToGenerator(latitude: number, longitude: number, appType: 'google' | 'waze' = 'google'): void {
    let url: string;

    if (appType === 'waze') {
      url = `https://waze.com/ul?ll=${latitude},${longitude}&navigate=yes`;
    } else {
      url = `https://www.google.com/maps/dir/?api=1&destination=${latitude},${longitude}`;
    }

    window.open(url, '_blank');
  }

  /**
   * Generar URL de navegación para toda la ruta optimizada
   */
  navigateOptimizedRoute(waypoints: any[]): void {
    if (waypoints.length === 0) return;

    const origin = `${waypoints[0].latitude},${waypoints[0].longitude}`;
    const destination = waypoints.length > 1
      ? `${waypoints[waypoints.length - 1].latitude},${waypoints[waypoints.length - 1].longitude}`
      : origin;

    let url = `https://www.google.com/maps/dir/${origin}/${destination}`;

    // Agregar waypoints intermedios si hay más de 2 puntos
    if (waypoints.length > 2) {
      const intermediateWaypoints = waypoints.slice(1, -1)
        .map((wp: any) => `${wp.latitude},${wp.longitude}`)
        .join('/');
      url = `https://www.google.com/maps/dir/${origin}/${intermediateWaypoints}/${destination}`;
    }

    window.open(url, '_blank');
  }

  // ===================================================
  // MÉTODOS UTILITARIOS
  // ===================================================

  /**
   * Calcular distancia entre dos puntos usando fórmula Haversine
   */
  private calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371; // Radio de la Tierra en kilómetros
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

  private formatDistance(distanceKm: number): string {
    if (distanceKm < 1) {
      return `${Math.round(distanceKm * 1000)} m`;
    } else {
      return `${distanceKm.toFixed(1)} km`;
    }
  }

  private formatTime(minutes: number): string {
    if (minutes < 60) {
      return `${minutes} min`;
    } else {
      const hours = Math.floor(minutes / 60);
      const remainingMinutes = minutes % 60;
      return `${hours}h ${remainingMinutes}min`;
    }
  }

  /**
   * Verificar si el dispositivo tiene GPS disponible
   */
  isGeolocationAvailable(): boolean {
    return 'geolocation' in navigator;
  }

  /**
   * Obtener precisión recomendada para la ubicación
   */
  getRecommendedLocationAccuracy(): number {
    // Retornar precisión en metros (50m es buena para rutas)
    return 50;
  }
  closeRoadmap(request: RoadmapCloseRequest): Observable<RoadmapCloseResponse> {
    return new Observable(observer => {
      this.getCurrentEmployeeInfo().subscribe({
        next: (employeeInfo) => {
          if (employeeInfo.error) {
            observer.error(employeeInfo.error);
            return;
          }

          console.log('🏁 Cerrando hoja de ruta:', {
            employeeId: employeeInfo.employeeId,
            roadmapId: request.roadmapId,
            returnHour: request.returnHour,
            finalKm: request.finalKm,
            observations: request.observations
          });

          this.http.post<RoadmapCloseResponse>(
            `${this.baseUrl}/${employeeInfo.employeeId}/roadmap/close`,
            request
          ).subscribe({
            next: (response) => {
              console.log('✅ Hoja de ruta cerrada exitosamente:', response);
              observer.next(response);
              observer.complete();
            },
            error: (error) => {
              console.error('❌ Error cerrando hoja de ruta:', error);
              observer.error(error);
            }
          });
        },
        error: (error) => {
          observer.error(error);
        }
      });
    });
  }

  /**
   * Obtener hojas de ruta que se pueden cerrar (EN_PROGRESO y COMPLETADA)
   */
  getCloseableRoadmaps(): Observable<EmployeeRoadmapResponse[]> {
    return new Observable(observer => {
      this.getCurrentEmployeeInfo().subscribe({
        next: (employeeInfo) => {
          if (employeeInfo.error) {
            observer.error(employeeInfo.error);
            return;
          }

          // Usar el endpoint de rutas activas como base para cerrar
          this.http.get<EmployeeRoadmapResponse[]>(`${this.baseUrl}/${employeeInfo.employeeId}/roadmaps`)
            .subscribe({
              next: (roadmaps) => {
                // Filtrar solo las que se pueden cerrar (cualquier estado excepto CERRADA)
                const closeableRoadmaps = roadmaps.filter(roadmap => {
                  // Determinar si se puede cerrar basado en el progreso
                  const progress = this.calculateRoadmapProgress(roadmap);
                  return progress.total > 0; // Si tiene precintos asignados, se puede cerrar
                });

                observer.next(closeableRoadmaps);
                observer.complete();
              },
              error: (error) => {
                observer.error(error);
              }
            });
        },
        error: (error) => {
          observer.error(error);
        }
      });
    });
  }

  /**
   * Calcular progreso de una hoja de ruta (método utilitario)
   */
  private calculateRoadmapProgress(roadmap: EmployeeRoadmapResponse): { completed: number, total: number, percentage: number } {
    const total = roadmap.generators.reduce((sum, gen) => sum + gen.totalBags, 0);
    const completed = roadmap.generators.reduce((sum, gen) => sum + gen.collectedBags, 0);
    const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;

    return { completed, total, percentage };
  }
}
