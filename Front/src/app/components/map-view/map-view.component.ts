// map-view.component.ts - VERSIÓN ACTUALIZADA CON RUTAS REALES
import { Component, OnInit, OnDestroy, Inject, AfterViewInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import * as L from 'leaflet';
import { MapService, MapLocation, RouteData } from "../../services/map.service";
import { RouteService, RealRouteData } from "../../services/Route/route.service"; // 🆕 NUEVA IMPORTACIÓN
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatIcon } from '@angular/material/icon';
import { MatButton } from '@angular/material/button';
import {DecimalPipe, NgForOf, NgIf} from '@angular/common';
import { MatSnackBar } from '@angular/material/snack-bar';

// Interface para los datos del generador
export interface MapDialogData {
  generator: {
    id: number;
    name: string;
    address: string;
    latitude: number;
    longitude: number;
    status: string;
  };
}

@Component({
  selector: 'app-map-view',
  standalone: true,
  imports: [
    MatProgressSpinner,
    MatIcon,
    MatButton,
    NgIf,
    DecimalPipe,
    NgForOf
  ],
  template: `
    <div class="map-dialog-container">
      <!-- Header -->
      <div class="map-header">
        <div class="generator-info">
          <h3>
            <mat-icon>business</mat-icon>
            {{ data.generator.name }}
          </h3>
          <p class="address">
            <mat-icon>place</mat-icon>
            {{ data.generator.address }}
          </p>
        </div>
        <button mat-icon-button (click)="closeDialog()" class="close-button">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Map container -->
      <div class="map-container">
        <div id="map" class="map-view"></div>

        <!-- Loading overlay -->
        <div *ngIf="loading" class="loading-overlay">
          <mat-spinner diameter="40"></mat-spinner>
          <p>{{ loadingMessage }}</p>
        </div>

        <!-- Route info panel -->
        <div *ngIf="routeInfo && !loading" class="route-info-panel">
          <!-- 🆕 NUEVO: Mostrar si es ruta real o aproximada -->
          <div class="route-type-indicator">
            <mat-icon [color]="isRealRoute ? 'primary' : 'warn'">
              {{ isRealRoute ? 'alt_route' : 'timeline' }}
            </mat-icon>
            <span class="route-type">{{ isRealRoute ? 'Ruta Real' : 'Ruta Aproximada' }}</span>
          </div>

          <div class="route-stats">
            <div class="stat">
              <mat-icon>straighten</mat-icon>
              <span class="label">Distancia</span>
              <span class="value">{{ formatDistance(routeInfo.distance) }}</span>
            </div>
            <div class="stat" *ngIf="routeInfo.duration > 0">
              <mat-icon>schedule</mat-icon>
              <span class="label">Tiempo</span>
              <span class="value">{{ formatTime(routeInfo.duration) }}</span>
            </div>
          </div>

          <!-- 🆕 NUEVO: Mostrar instrucciones si están disponibles -->
          <div class="route-instructions" *ngIf="realRouteData && realRouteData.instructions.length > 0">
            <h5>Primeras instrucciones:</h5>
            <div class="instruction" *ngFor="let instruction of realRouteData.instructions.slice(0, 2)">
              <mat-icon>navigation</mat-icon>
              <span>{{ instruction.instruction }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="map-actions">
        <button mat-raised-button color="primary" (click)="centerOnEmployee()" [disabled]="loading">
          <mat-icon>my_location</mat-icon>
          Mi Ubicación
        </button>

        <button mat-raised-button color="accent" (click)="centerOnGenerator()">
          <mat-icon>business</mat-icon>
          Ver Generador
        </button>

        <!-- 🆕 NUEVO: Botón para recalcular con ruta real -->
        <button mat-stroked-button color="primary" (click)="recalculateRealRoute()" [disabled]="loading || isCalculatingRoute">
          <mat-icon *ngIf="!isCalculatingRoute">refresh</mat-icon>
          <mat-spinner *ngIf="isCalculatingRoute" diameter="20"></mat-spinner>
          {{ isCalculatingRoute ? 'Calculando...' : 'Ruta Real' }}
        </button>

        <button mat-raised-button color="warn" (click)="openInGoogleMaps()" *ngIf="data.generator.latitude">
          <mat-icon>navigation</mat-icon>
          Abrir en Google Maps
        </button>
      </div>
    </div>
  `,
  styles: [`
    .map-dialog-container {
      width: 90vw;
      max-width: 800px;
      height: 80vh;
      max-height: 600px;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .map-header {
      padding: 16px 20px;
      background: linear-gradient(135deg, #1e88e5 0%, #1565c0 100%);
      color: white;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .generator-info h3 {
      margin: 0 0 8px 0;
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 1.3rem;
      font-weight: 600;
    }

    .address {
      margin: 0;
      display: flex;
      align-items: center;
      gap: 6px;
      opacity: 0.9;
      font-size: 0.9rem;
    }

    .close-button {
      color: white;
    }

    .map-container {
      flex: 1;
      position: relative;
      overflow: hidden;
    }

    .map-view {
      width: 100%;
      height: 100%;
    }

    .loading-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.9);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .loading-overlay p {
      margin-top: 16px;
      color: #666;
      font-size: 0.9rem;
    }

    .route-info-panel {
      position: absolute;
      top: 16px;
      right: 16px;
      background: white;
      border-radius: 8px;
      padding: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      z-index: 1000;
      min-width: 200px;
    }

    /* 🆕 NUEVOS ESTILOS */
    .route-type-indicator {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 8px;
      padding-bottom: 8px;
      border-bottom: 1px solid #eee;
    }

    .route-type {
      font-size: 0.8rem;
      font-weight: 600;
    }

    .route-instructions {
      margin-top: 8px;
      padding-top: 8px;
      border-top: 1px solid #eee;
    }

    .route-instructions h5 {
      margin: 0 0 6px 0;
      font-size: 0.8rem;
      color: #666;
    }

    .instruction {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 4px;
      font-size: 0.75rem;
      color: #555;
    }

    .instruction mat-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
      color: #1565c0;
    }

    .route-stats {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .stat {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .stat mat-icon {
      color: #1565c0;
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .stat .label {
      flex: 1;
      font-size: 0.85rem;
      color: #666;
    }

    .stat .value {
      font-weight: 600;
      color: #333;
      font-size: 0.9rem;
    }

    .map-actions {
      padding: 16px 20px;
      background: #f5f5f5;
      display: flex;
      gap: 12px;
      justify-content: center;
      flex-wrap: wrap;
    }

    .map-actions button {
      min-width: 120px;
    }

    /* Responsive */
    @media (max-width: 600px) {
      .map-dialog-container {
        width: 100vw;
        height: 100vh;
        max-width: none;
        max-height: none;
      }

      .map-header {
        padding: 12px 16px;
      }

      .generator-info h3 {
        font-size: 1.1rem;
      }

      .route-info-panel {
        top: 8px;
        right: 8px;
        left: 8px;
        min-width: auto;
      }

      .route-stats {
        flex-direction: row;
        justify-content: space-around;
      }

      .map-actions {
        padding: 12px 16px;
        gap: 8px;
      }

      .map-actions button {
        min-width: 80px;
        flex: 1;
      }
    }
  `]
})
export class MapViewComponent implements OnInit, AfterViewInit, OnDestroy {
  map!: L.Map;
  loading = true;
  loadingMessage = 'Inicializando mapa...';

  employeeLocation: MapLocation | null = null;
  generatorLocation: MapLocation;
  routeInfo: RouteData | null = null;

  // 🆕 NUEVAS PROPIEDADES PARA RUTAS REALES
  realRouteData: RealRouteData | null = null;
  isRealRoute = false;
  isCalculatingRoute = false;

  employeeMarker: L.Marker | null = null;
  generatorMarker: L.Marker | null = null;
  routePolyline: L.Polyline | null = null;

  private locationSubscription?: Subscription;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: MapDialogData,
    private dialogRef: MatDialogRef<MapViewComponent>,
    private mapService: MapService,
    private routeService: RouteService, // 🆕 NUEVA INYECCIÓN
    private snackBar: MatSnackBar
  ) {
    this.generatorLocation = {
      latitude: data.generator.latitude,
      longitude: data.generator.longitude
    };
  }

  ngOnInit(): void {
    // Configurar iconos de Leaflet
    MapService.configureLeafletIcons();
  }

  ngAfterViewInit(): void {
    // Dar tiempo para que se renderice el DOM
    setTimeout(() => {
      this.initializeMap();
    }, 100);
  }

  ngOnDestroy(): void {
    if (this.locationSubscription) {
      this.locationSubscription.unsubscribe();
    }
    if (this.map) {
      this.map.remove();
    }
  }

  private initializeMap(): void {
    try {
      // Inicializar mapa centrado en el generador
      this.map = L.map('map', {
        center: [this.generatorLocation.latitude, this.generatorLocation.longitude],
        zoom: 15,
        zoomControl: true
      });

      // Agregar capa de OpenStreetMap
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '© OpenStreetMap contributors'
      }).addTo(this.map);

      // Agregar marcador del generador
      this.addGeneratorMarker();

      // Obtener ubicación del empleado y calcular ruta
      this.loadEmployeeLocationAndRoute();

    } catch (error) {
      console.error('Error initializing map:', error);
      this.showError('Error al inicializar el mapa');
    }
  }

  private addGeneratorMarker(): void {
    const icon = this.mapService.createCustomIcon('generator', this.data.generator.status);

    this.generatorMarker = L.marker(
      [this.generatorLocation.latitude, this.generatorLocation.longitude],
      { icon }
    ).addTo(this.map);

    this.generatorMarker.bindPopup(`
      <div style="min-width: 200px;">
        <h4 style="margin: 0 0 8px 0; color: #1565c0;">
          🏢 ${this.data.generator.name}
        </h4>
        <p style="margin: 0 0 4px 0; font-size: 0.9rem;">
          📍 ${this.data.generator.address}
        </p>
        <p style="margin: 0; font-size: 0.8rem; color: #666;">
          Estado: ${this.data.generator.status}
        </p>
      </div>
    `).openPopup();
  }

  private loadEmployeeLocationAndRoute(): void {
    this.loadingMessage = 'Obteniendo tu ubicación...';

    // Intentar obtener ubicación actual
    this.mapService.getCurrentLocation().subscribe({
      next: (location) => {
        console.log('✅ Ubicación del empleado obtenida:', location);
        this.employeeLocation = location;
        this.addEmployeeMarker();

        // 🆕 CAMBIO: Intentar ruta real primero
        this.calculateRealRoute();

        this.startLocationTracking();
      },
      error: (error) => {
        console.warn('⚠️ Error getting employee location:', error);

        // Fallback: Usar ubicación de respaldo o la ubicación actual del componente padre
        const fallbackLocation = this.mapService.getCurrentLocationValue();
        if (fallbackLocation) {
          console.log('🏠 Usando ubicación de respaldo del empleado');
          this.employeeLocation = fallbackLocation;
          this.addEmployeeMarker();
          this.calculateRealRoute(); // 🆕 CAMBIO: Intentar ruta real
          this.loading = false;
        } else {
          // Si no hay ubicación, usar ubicación de Córdoba cerca del generador
          console.log('🌍 Usando ubicación aproximada');
          this.employeeLocation = {
            latitude: this.generatorLocation.latitude + 0.005, // 500m aproximadamente
            longitude: this.generatorLocation.longitude + 0.005,
            accuracy: 0
          };
          this.addEmployeeMarker();
          this.calculateAndDisplayRoute(); // Usar método original como fallback
          this.loading = false;
          this.showError('Usando ubicación aproximada - habilita GPS para mayor precisión');
        }
      }
    });
  }

  private addEmployeeMarker(): void {
    if (!this.employeeLocation) return;

    const icon = this.mapService.createCustomIcon('employee');

    this.employeeMarker = L.marker(
      [this.employeeLocation.latitude, this.employeeLocation.longitude],
      { icon }
    ).addTo(this.map);

    this.employeeMarker.bindPopup(`
      <div style="text-align: center;">
        <h4 style="margin: 0 0 8px 0; color: #4caf50;">
          👤 Tu ubicación
        </h4>
        <p style="margin: 0; font-size: 0.8rem; color: #666;">
          Precisión: ±${this.employeeLocation.accuracy?.toFixed(0) || '?'} metros
        </p>
      </div>
    `);
  }

  // 🆕 NUEVO MÉTODO: Calcular ruta real usando RouteService
  private calculateRealRoute(): void {
    if (!this.employeeLocation) return;

    this.loadingMessage = 'Calculando ruta real...';
    this.isCalculatingRoute = true;

    const from: [number, number] = [this.employeeLocation.latitude, this.employeeLocation.longitude];
    const to: [number, number] = [this.generatorLocation.latitude, this.generatorLocation.longitude];

    this.routeService.getRealRoute(from, to).subscribe({
      next: (realRoute) => {
        console.log('✅ Ruta real obtenida:', realRoute);
        this.realRouteData = realRoute;
        this.isRealRoute = true;

        // Convertir datos para compatibilidad con la interfaz existente
        this.routeInfo = {
          distance: realRoute.distance / 1000, // Convertir metros a kilómetros
          duration: realRoute.duration / 60, // Convertir segundos a minutos
          coordinates: realRoute.coordinates
        };

        this.displayRealRoute(realRoute);
        this.fitMapToBounds();
        this.loading = false;
        this.isCalculatingRoute = false;

        this.showSuccess('🗺️ Ruta real calculada correctamente');
      },
      error: (error) => {
        console.warn('⚠️ Error calculating real route, falling back to simple route:', error);
        this.isRealRoute = false;
        this.calculateAndDisplayRoute(); // Fallback al método original
        this.isCalculatingRoute = false;
        this.showError('Usando ruta aproximada - servicio de rutas no disponible');
      }
    });
  }

  // 🆕 NUEVO MÉTODO: Mostrar ruta real en el mapa
  private displayRealRoute(route: RealRouteData): void {
    if (this.routePolyline) {
      this.map.removeLayer(this.routePolyline);
    }

    this.routePolyline = L.polyline(route.coordinates, {
      color: '#1565c0',
      weight: 5,
      opacity: 0.8,
      smoothFactor: 1
    }).addTo(this.map);

    // Agregar popup con información de la ruta
    this.routePolyline.bindPopup(`
      <div style="text-align: center;">
        <h5 style="margin: 0 0 4px 0;">🗺️ Ruta Real</h5>
        <p style="margin: 0; font-size: 0.8rem;">
          📏 ${this.formatDistance(route.distance)}<br>
          ⏱️ ${this.formatDuration(route.duration)}
        </p>
      </div>
    `);
  }

  // Método original mantenido como fallback
  private calculateAndDisplayRoute(): void {
    if (!this.employeeLocation) return;

    this.loadingMessage = 'Calculando ruta...';

    this.mapService.calculateRoute(this.employeeLocation, this.generatorLocation).subscribe({
      next: (route) => {
        this.routeInfo = route;
        this.isRealRoute = false;
        this.displayRoute(route);
        this.fitMapToBounds();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error calculating route:', error);
        this.loading = false;
        this.showError('No se pudo calcular la ruta');
      }
    });
  }

  private displayRoute(route: RouteData): void {
    if (this.routePolyline) {
      this.map.removeLayer(this.routePolyline);
    }

    this.routePolyline = L.polyline(route.coordinates, {
      color: '#ff9800',
      weight: 4,
      opacity: 0.8,
      dashArray: '5, 10'
    }).addTo(this.map);
  }

  private fitMapToBounds(): void {
    if (this.employeeLocation && this.generatorLocation) {
      const bounds = L.latLngBounds([
        [this.employeeLocation.latitude, this.employeeLocation.longitude],
        [this.generatorLocation.latitude, this.generatorLocation.longitude]
      ]);

      this.map.fitBounds(bounds, {
        padding: [50, 50],
        maxZoom: 16
      });
    }
  }

  private startLocationTracking(): void {
    this.locationSubscription = this.mapService.startLocationTracking().subscribe({
      next: (location) => {
        this.employeeLocation = location;

        if (this.employeeMarker) {
          this.employeeMarker.setLatLng([location.latitude, location.longitude]);
        }

        // Recalcular ruta si se mueve más de 100 metros
        if (this.shouldRecalculateRoute(location)) {
          if (this.isRealRoute) {
            this.calculateRealRoute();
          } else {
            this.calculateAndDisplayRoute();
          }
        }
      },
      error: (error) => {
        console.error('Error tracking location:', error);
      }
    });
  }

  private shouldRecalculateRoute(newLocation: MapLocation): boolean {
    if (!this.employeeLocation) return true;

    const distance = this.mapService.calculateDistance(
      this.employeeLocation.latitude,
      this.employeeLocation.longitude,
      newLocation.latitude,
      newLocation.longitude
    );

    return distance > 0.1; // Recalcular si se movió más de 100 metros
  }

  // ===================================================
  // 🆕 NUEVOS MÉTODOS PÚBLICOS
  // ===================================================

  /**
   * Recalcular con ruta real (botón)
   */
  recalculateRealRoute(): void {
    if (!this.employeeLocation) {
      this.showError('Ubicación no disponible');
      return;
    }

    this.calculateRealRoute();
  }

  /**
   * Formatear distancia (compatible con metros y kilómetros)
   */
  formatDistance(distance: number): string {
    if (distance > 1000) {
      // Es en metros, convertir a km
      return this.routeService.formatDistance(distance, true);
    } else {
      // Es en km
      return this.routeService.formatDistance(distance * 1000, true);
    }
  }

  /**
   * Formatear duración (compatible con segundos y minutos)
   */
  formatDuration(duration: number): string {
    if (duration > 100) {
      // Es en segundos
      return this.routeService.formatDuration(duration);
    } else {
      // Es en minutos
      return this.routeService.formatTime(duration);
    }
  }

  // ===================================================
  // MÉTODOS PÚBLICOS EXISTENTES (sin cambios)
  // ===================================================

  centerOnEmployee(): void {
    if (this.employeeLocation) {
      this.map.setView([this.employeeLocation.latitude, this.employeeLocation.longitude], 16);
      if (this.employeeMarker) {
        this.employeeMarker.openPopup();
      }
    } else {
      this.showError('Ubicación del empleado no disponible');
    }
  }

  centerOnGenerator(): void {
    this.map.setView([this.generatorLocation.latitude, this.generatorLocation.longitude], 16);
    if (this.generatorMarker) {
      this.generatorMarker.openPopup();
    }
  }

  openInGoogleMaps(): void {
    let url: string;

    if (this.realRouteData && this.isRealRoute) {
      // Usar coordenadas de ruta real
      const from = this.realRouteData.coordinates[0];
      const to = this.realRouteData.coordinates[this.realRouteData.coordinates.length - 1];
      url = this.routeService.generateSimpleGoogleMapsUrl([from[0], from[1]], [to[0], to[1]]);
    } else {
      // Usar coordenadas simples
      url = `https://www.google.com/maps/dir/${this.employeeLocation?.latitude},${this.employeeLocation?.longitude}/${this.generatorLocation.latitude},${this.generatorLocation.longitude}`;
    }

    window.open(url, '_blank');
  }

  formatTime(minutes: number): string {
    return this.mapService.formatTime(minutes);
  }

  closeDialog(): void {
    this.dialogRef.close();
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Cerrar', {
      duration: 4000,
      panelClass: ['error-snackbar']
    });
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Cerrar', {
      duration: 3000,
      panelClass: ['success-snackbar']
    });
  }
}
