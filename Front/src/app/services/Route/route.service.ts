// route.service.ts - VERSIÓN CORREGIDA COMPLETA
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

// ===================================================
// INTERFACES EXISTENTES (mantener)
// ===================================================

export interface OptimizedRouteResponse {
  roadmapId: number;
  zone: string;
  waypoints: RouteWaypoint[];
  summary: RouteSummary;
  status: string;
}

export interface RouteWaypoint {
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
  status: string; // PENDING, VISITED, SKIPPED
}

export interface RouteSummary {
  totalDistance: number; // en kilómetros
  totalTimeMinutes: number;
  totalGenerators: number;
  totalBags: number;
  startTime: string;
  estimatedEndTime: string;
  optimizationMethod: string;
}

export interface RouteOptimizationRequest {
  roadmapId: number;
  startLatitude: number;
  startLongitude: number;
  startTime: string;
  includeReturnToDepot: boolean;
  optimizationMethod: string;
}

export interface GeocodingStats {
  generatorsWithoutCoordinates: number;
  addressesWithoutCoordinates: number;
  sampleAddresses: string[];
}

// ===================================================
// 🆕 NUEVAS INTERFACES PARA RUTAS REALES
// ===================================================

export interface RealRouteData {
  coordinates: [number, number][];
  distance: number; // en metros
  duration: number; // en segundos
  instructions: RouteInstruction[];
  bbox: [number, number, number, number];
}

export interface RouteInstruction {
  instruction: string;
  distance: number;
  time: number;
  wayIndex: number;
}

export interface OptimizedMultiStopRoute {
  totalDistance: number;
  totalDuration: number;
  routes: RouteSegment[];
  orderedWaypoints: SimpleWaypoint[];
}

export interface RouteSegment {
  from: SimpleWaypoint;
  to: SimpleWaypoint;
  coordinates: [number, number][];
  distance: number;
  duration: number;
}

export interface SimpleWaypoint {
  id: number;
  name: string;
  latitude: number;
  longitude: number;
  order: number;
  estimatedArrival?: string;
}

@Injectable({
  providedIn: 'root'
})
export class RouteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8081/api/routes';

  // 🆕 URLs para APIs de routing externas (gratuitas)
  private readonly OSRM_BASE_URL = 'https://router.project-osrm.org/route/v1/driving/';
  private readonly OSRM_TABLE_URL = 'https://router.project-osrm.org/table/v1/driving/';

  // ===================================================
  // MÉTODOS EXISTENTES DE OPTIMIZACIÓN (mantener)
  // ===================================================

  /**
   * Obtener ruta optimizada para una hoja de ruta
   */
  getOptimizedRoute(roadmapId: number): Observable<OptimizedRouteResponse> {
    return this.http.get<OptimizedRouteResponse>(`${this.apiUrl}/${roadmapId}/optimize`);
  }

  /**
   * Optimizar ruta con parámetros personalizados
   */
  optimizeRouteWithParams(request: RouteOptimizationRequest): Observable<OptimizedRouteResponse> {
    return this.http.post<OptimizedRouteResponse>(`${this.apiUrl}/optimize`, request);
  }

  // ===================================================
  // MÉTODOS EXISTENTES DE GEOCODIFICACIÓN (mantener)
  // ===================================================

  /**
   * Geocodificar todos los generadores sin coordenadas
   */
  geocodeAllGenerators(): Observable<any> {
    return this.http.post(`${this.apiUrl}/geocode/all`, {});
  }

  /**
   * Geocodificar un generador específico
   */
  geocodeSpecificGenerator(generatorId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/geocode/${generatorId}`, {});
  }

  /**
   * Obtener estadísticas de geocodificación
   */
  getGeocodingStats(): Observable<GeocodingStats> {
    return this.http.get<GeocodingStats>(`${this.apiUrl}/geocoding/stats`);
  }

  /**
   * Validar si una dirección es geocodificable
   */
  validateAddress(address: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/validate-address`, {
      params: { address }
    });
  }

  /**
   * Obtener coordenadas de una dirección
   */
  geocodeAddress(address: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/geocode`, {
      params: { address }
    });
  }

  // ===================================================
  // 🆕 NUEVOS MÉTODOS PARA RUTAS REALES
  // ===================================================

  /**
   * Obtener ruta real entre dos puntos usando OSRM (gratuito)
   */
  getRealRoute(from: [number, number], to: [number, number]): Observable<RealRouteData> {
    const coordinates = `${from[1]},${from[0]};${to[1]},${to[0]}`;
    const url = `${this.OSRM_BASE_URL}${coordinates}?overview=full&geometries=geojson&steps=true`;

    return this.http.get<any>(url).pipe(
      map(response => {
        if (response.routes && response.routes.length > 0) {
          const route = response.routes[0];

          return {
            coordinates: route.geometry.coordinates.map((coord: number[]) => [coord[1], coord[0]]),
            distance: route.distance,
            duration: route.duration,
            instructions: this.parseOSRMInstructions(route.legs[0]?.steps || []),
            bbox: this.calculateBBox(route.geometry.coordinates)
          };
        }
        throw new Error('No route found');
      }),
      catchError(error => {
        console.warn('OSRM failed, using direct line:', error);
        return this.getDirectRoute(from, to);
      })
    );
  }

  /**
   * Optimizar ruta con múltiples paradas usando OSRM
   */
  getOptimizedMultiStopRoute(
    start: [number, number],
    waypoints: SimpleWaypoint[],
    returnToStart: boolean = false
  ): Observable<OptimizedMultiStopRoute> {

    if (waypoints.length === 0) {
      return of({
        totalDistance: 0,
        totalDuration: 0,
        routes: [],
        orderedWaypoints: []
      });
    }

    // Preparar coordenadas para OSRM
    const allCoords = [start, ...waypoints.map(wp => [wp.latitude, wp.longitude] as [number, number])];
    if (returnToStart) {
      allCoords.push(start);
    }

    const coordinatesString = allCoords.map(coord => `${coord[1]},${coord[0]}`).join(';');

    // Usar OSRM Table service para optimización
    const tableUrl = `${this.OSRM_TABLE_URL}${coordinatesString}`;

    return this.http.get<any>(tableUrl).pipe(
      map(response => {
        // Implementar algoritmo de optimización simple (Nearest Neighbor)
        const optimizedOrder = this.optimizeWaypointOrder(response.distances, waypoints);
        return this.calculateSegmentedRouteSync(start, optimizedOrder, returnToStart);
      }),
      catchError((error: any) => { // ✅ FIX: Tipar explícitamente el error
        console.warn('Route optimization failed, using original order:', error);
        return of(this.calculateSegmentedRouteSync(start, waypoints, returnToStart)); // ✅ FIX: Usar 'of()' para retornar Observable
      })
    );
  }

  /**
   * Crear waypoints simples desde RouteWaypoint (compatibilidad)
   */
  convertToSimpleWaypoints(routeWaypoints: RouteWaypoint[]): SimpleWaypoint[] {
    return routeWaypoints.map(rw => ({
      id: rw.generatorId,
      name: rw.generatorName,
      latitude: rw.latitude,
      longitude: rw.longitude,
      order: rw.order
    }));
  }

  /**
   * Calcular ruta por segmentos de forma síncrona (para optimización rápida)
   */
  private calculateSegmentedRouteSync(
    start: [number, number],
    waypoints: SimpleWaypoint[],
    returnToStart: boolean
  ): OptimizedMultiStopRoute {
    const routes: RouteSegment[] = [];
    let currentPoint = start;
    let totalDistance = 0;
    let totalDuration = 0;

    // Crear segmentos basados en distancia euclidiana
    waypoints.forEach((waypoint, index) => {
      const nextPoint: [number, number] = [waypoint.latitude, waypoint.longitude];
      const distance = this.calculateDistance(
        currentPoint[0], currentPoint[1],
        nextPoint[0], nextPoint[1]
      ) * 1000; // convertir a metros

      const duration = (distance / 1000 / 40) * 3600; // 40 km/h promedio en segundos

      const fromWaypoint = index === 0
        ? { id: 0, name: 'Punto de inicio', latitude: start[0], longitude: start[1], order: 0 }
        : waypoints[index - 1];

      routes.push({
        from: fromWaypoint,
        to: waypoint,
        coordinates: [currentPoint, nextPoint], // Línea directa por simplicidad
        distance: distance,
        duration: duration
      });

      totalDistance += distance;
      totalDuration += duration;
      currentPoint = nextPoint;
    });

    // Si necesitamos regresar al inicio
    if (returnToStart && waypoints.length > 0) {
      const lastWaypoint = waypoints[waypoints.length - 1];
      const returnDistance = this.calculateDistance(
        lastWaypoint.latitude, lastWaypoint.longitude,
        start[0], start[1]
      ) * 1000;

      const returnDuration = (returnDistance / 1000 / 40) * 3600;

      routes.push({
        from: lastWaypoint,
        to: { id: 0, name: 'Punto de inicio', latitude: start[0], longitude: start[1], order: waypoints.length + 1 },
        coordinates: [[lastWaypoint.latitude, lastWaypoint.longitude], start],
        distance: returnDistance,
        duration: returnDuration
      });

      totalDistance += returnDistance;
      totalDuration += returnDuration;
    }

    return {
      totalDistance,
      totalDuration,
      routes,
      orderedWaypoints: waypoints
    };
  }

  /**
   * Optimizar orden de waypoints usando algoritmo Nearest Neighbor
   */
  private optimizeWaypointOrder(distanceMatrix: number[][], waypoints: SimpleWaypoint[]): SimpleWaypoint[] {
    if (!distanceMatrix || waypoints.length <= 1) return waypoints;

    const optimized: SimpleWaypoint[] = [];
    const unvisited = [...waypoints];
    let currentIndex = 0; // Empezar desde el punto de inicio (índice 0 en la matriz)

    while (unvisited.length > 0) {
      let nearestIndex = 0;
      let shortestDistance = Infinity;

      // Encontrar el punto más cercano no visitado
      unvisited.forEach((waypoint, idx) => {
        const waypointIndex = waypoints.indexOf(waypoint) + 1; // +1 porque el inicio es índice 0
        if (distanceMatrix[currentIndex] && distanceMatrix[currentIndex][waypointIndex] !== undefined) {
          const distance = distanceMatrix[currentIndex][waypointIndex];

          if (distance < shortestDistance) {
            shortestDistance = distance;
            nearestIndex = idx;
          }
        }
      });

      // Agregar el punto más cercano a la ruta optimizada
      const nearestWaypoint = unvisited[nearestIndex];
      nearestWaypoint.order = optimized.length + 1;
      optimized.push(nearestWaypoint);

      // Actualizar índice actual y eliminar de no visitados
      currentIndex = waypoints.indexOf(nearestWaypoint) + 1;
      unvisited.splice(nearestIndex, 1);
    }

    return optimized;
  }

  /**
   * Ruta directa (fallback)
   */
  private getDirectRoute(from: [number, number], to: [number, number]): Observable<RealRouteData> {
    const distance = this.calculateDistance(from[0], from[1], to[0], to[1]);
    const duration = (distance / 40) * 3600; // 40 km/h promedio

    return of({
      coordinates: [from, to],
      distance: distance * 1000, // convertir a metros
      duration: duration,
      instructions: [{
        instruction: `Dirigirse hacia ${to[0].toFixed(4)}, ${to[1].toFixed(4)}`,
        distance: distance * 1000,
        time: duration,
        wayIndex: 0
      }],
      bbox: [
        Math.min(from[1], to[1]), Math.min(from[0], to[0]),
        Math.max(from[1], to[1]), Math.max(from[0], to[0])
      ]
    });
  }

  /**
   * Parsear instrucciones de OSRM
   */
  private parseOSRMInstructions(steps: any[]): RouteInstruction[] {
    return steps.map((step, index) => ({
      instruction: step.maneuver?.instruction || `Continuar por ${step.name || 'la calle'}`,
      distance: step.distance || 0,
      time: step.duration || 0,
      wayIndex: index
    }));
  }

  /**
   * Calcular bounding box
   */
  private calculateBBox(coordinates: number[][]): [number, number, number, number] {
    const longs = coordinates.map(coord => coord[0]);
    const lats = coordinates.map(coord => coord[1]);

    return [
      Math.min(...longs),
      Math.min(...lats),
      Math.max(...longs),
      Math.max(...lats)
    ];
  }

  // ===================================================
  // MÉTODOS EXISTENTES MEJORADOS
  // ===================================================

  /**
   * Calcular distancia entre dos puntos (fórmula Haversine)
   */
  calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371; // Radio de la Tierra en kilómetros
    const dLat = this.toRadians(lat2 - lat1);
    const dLon = this.toRadians(lon2 - lon1);

    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRadians(lat1)) * Math.cos(this.toRadians(lat2)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c;
  }

  /**
   * Convertir grados a radianes
   */
  private toRadians(degrees: number): number {
    return degrees * (Math.PI / 180);
  }

  /**
   * Formatear distancia para mostrar
   */
  formatDistance(distanceValue: number, isMeters: boolean = false): string {
    let distanceKm: number;

    if (isMeters) {
      distanceKm = distanceValue / 1000;
    } else {
      distanceKm = distanceValue;
    }

    if (distanceKm < 1) {
      return `${Math.round(distanceKm * 1000)} m`;
    } else {
      return `${distanceKm.toFixed(1)} km`;
    }
  }

  /**
   * Formatear tiempo en minutos a formato legible
   */
  formatTime(timeValue: number, isSeconds: boolean = false): string {
    let minutes: number;

    if (isSeconds) {
      minutes = Math.round(timeValue / 60);
    } else {
      minutes = timeValue;
    }

    if (minutes < 60) {
      return `${minutes} min`;
    } else {
      const hours = Math.floor(minutes / 60);
      const remainingMinutes = minutes % 60;
      return `${hours}h ${remainingMinutes}min`;
    }
  }

  /**
   * 🆕 Formatear duración (segundos a formato legible)
   */
  formatDuration(seconds: number): string {
    return this.formatTime(seconds, true);
  }

  /**
   * Generar URL de Google Maps para navegación (método existente mejorado)
   */
  generateGoogleMapsUrl(waypoints: RouteWaypoint[]): string {
    if (waypoints.length === 0) return '';

    const origin = `${waypoints[0].latitude},${waypoints[0].longitude}`;
    const destination = waypoints.length > 1
      ? `${waypoints[waypoints.length - 1].latitude},${waypoints[waypoints.length - 1].longitude}`
      : origin;

    let url = `https://www.google.com/maps/dir/${origin}/${destination}`;

    // Agregar waypoints intermedios si hay más de 2 puntos
    if (waypoints.length > 2) {
      const intermediateWaypoints = waypoints.slice(1, -1)
        .map(wp => `${wp.latitude},${wp.longitude}`)
        .join('/');
      url = `https://www.google.com/maps/dir/${origin}/${intermediateWaypoints}/${destination}`;
    }

    return url;
  }

  /**
   * 🆕 Generar URL para Google Maps con ruta optimizada
   */
  generateGoogleMapsUrlForOptimizedRoute(optimizedRoute: OptimizedMultiStopRoute): string {
    if (optimizedRoute.orderedWaypoints.length === 0) return '';

    const origin = optimizedRoute.routes[0]?.from;
    const destination = optimizedRoute.orderedWaypoints[optimizedRoute.orderedWaypoints.length - 1];
    const waypoints = optimizedRoute.orderedWaypoints.slice(0, -1);

    let url = `https://www.google.com/maps/dir/${origin.latitude},${origin.longitude}`;

    waypoints.forEach(wp => {
      url += `/${wp.latitude},${wp.longitude}`;
    });

    url += `/${destination.latitude},${destination.longitude}`;

    return url;
  }

  /**
   * 🆕 Generar URL simple para Google Maps
   */
  generateSimpleGoogleMapsUrl(from: [number, number], to: [number, number]): string {
    return `https://www.google.com/maps/dir/${from[0]},${from[1]}/${to[0]},${to[1]}`;
  }

  /**
   * Generar URL de Waze para navegación
   */
  generateWazeUrl(latitude: number, longitude: number): string {
    return `https://waze.com/ul?ll=${latitude},${longitude}&navigate=yes`;
  }

  /**
   * Obtener color para el estado de un waypoint
   */
  getWaypointStatusColor(status: string): string {
    switch (status) {
      case 'PENDING': return '#ff9800'; // Naranja
      case 'VISITED': return '#4caf50'; // Verde
      case 'SKIPPED': return '#f44336'; // Rojo
      default: return '#9e9e9e'; // Gris
    }
  }

  /**
   * Obtener icono para el estado de un waypoint
   */
  getWaypointStatusIcon(status: string): string {
    switch (status) {
      case 'PENDING': return 'schedule';
      case 'VISITED': return 'check_circle';
      case 'SKIPPED': return 'cancel';
      default: return 'location_on';
    }
  }
}
