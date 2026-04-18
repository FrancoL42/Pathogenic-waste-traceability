// empleado-work.component.ts - VERSIÓN COMPLETA ACTUALIZADA PARA ÓRDENES
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { QrScannerComponent } from '../qr-scanner/qr-scanner.component';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';

// Importaciones existentes
import { EmployeeRoadmapResponse, GeneratorInRoadmap, ScanQRResponse } from "../../models/Interfaces";
import { EmpleadoService } from "../../services/Empleado/empleado.service";
import {
  RouteService,
  OptimizedRouteResponse,
  RouteSummary,
  SimpleWaypoint,
  RealRouteData,
  OptimizedMultiStopRoute
} from "../../services/Route/route.service";

import { MapService, MapLocation, LocationError, LocationErrorType} from "../../services/map.service";
import { MapViewComponent, MapDialogData } from '../map-view/map-view.component';

// Angular Material imports
import { MatIcon } from "@angular/material/icon";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
import {
  MatCard,
  MatCardActions,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle
} from "@angular/material/card";
import {DatePipe, DecimalPipe, NgForOf, NgIf} from "@angular/common";
import { MatProgressBar } from "@angular/material/progress-bar";
import { MatDivider } from "@angular/material/divider";
import { MatChip, MatChipSet } from "@angular/material/chips";
import {MatButton, MatIconButton} from "@angular/material/button";
import { MatTooltip } from "@angular/material/tooltip";

// Interfaces para ubicación y navegación
interface CurrentLocation {
  latitude: number;
  longitude: number;
  accuracy?: number;
}

interface NearestGenerator {
  name: string;
  distance: string;
  estimatedTime: string;
}

// 🆕 Interfaz para datos del mapa mejorado
export interface EnhancedMapDialogData {
  generator: {
    id: number;
    name: string;
    address: string;
    latitude: number;
    longitude: number;
    status: string;
  };
  allGenerators?: SimpleWaypoint[];
  showOptimizedRoute?: boolean;
}

@Component({
  selector: 'app-empleado-work',
  templateUrl: './empleado-work.component.html',
  standalone: true,
  imports: [
    MatIcon,
    MatProgressSpinner,
    MatCard,
    MatCardContent,
    DatePipe,
    MatProgressBar,
    MatDivider,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatChipSet,
    MatChip,
    MatCardActions,
    MatButton,
    MatTooltip,
    NgIf,
    NgForOf,
    DecimalPipe,
    MatIconButton
  ],
  styleUrls: ['./empleado-work.component.css']
})
export class EmpleadoWorkComponent implements OnInit, OnDestroy {
  roadmap: EmployeeRoadmapResponse | null = null;
  roadmapId: number = 0;
  loading = true;
  error = '';
  employeeName = '';
  Math = Math;

  // Propiedades para geolocalización y mapas
  loadingRoute = false;
  gettingLocation = false;
  currentLocation: CurrentLocation | null = null;
  nearestGenerator: NearestGenerator | null = null;
  routeSummary: RouteSummary | null = null;
  optimizedRoute: OptimizedRouteResponse | null = null;

  // Propiedades para manejo de errores de ubicación
  locationPermissionStatus: 'granted' | 'denied' | 'prompt' | 'unsupported' = 'prompt';
  locationErrorInfo: LocationError | null = null;
  usesFallbackLocation = false;

  // 🆕 NUEVAS PROPIEDADES PARA RUTAS MEJORADAS
  optimizedWaypoints: SimpleWaypoint[] = [];
  showOptimizedRoutes = false;
  routeOptimizationSupported = false;
  realRouteData: RealRouteData | null = null;
  optimizedMultiStopRoute: OptimizedMultiStopRoute | null = null;

  // Subscripciones para cleanup
  private locationSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private empleadoService: EmpleadoService,
    protected routeService: RouteService,
    private mapService: MapService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.roadmapId = Number(this.route.snapshot.paramMap.get('roadmapId'));
    this.loadEmployeeInfo();
    this.loadRoadmap();

    // Verificar permisos primero
    this.checkLocationPermissions().then(() => {
      // Solo después de verificar permisos, inicializar ubicación
      setTimeout(() => {
        this.initializeLocationTracking();
      }, 1000);
    });
  }

  ngOnDestroy(): void {
    // Limpiar subscripciones
    if (this.locationSubscription) {
      this.locationSubscription.unsubscribe();
    }
  }

  // ===================================================
  // ✅ MÉTODOS ACTUALIZADOS PARA TRABAJAR CON requestedBags
  // ===================================================

  /**
   * ✅ Obtener progreso total basado en requestedBags de órdenes
   */
  getTotalProgress(): { completed: number, total: number, percentage: number } {
    if (!this.roadmap?.generators || this.roadmap.generators.length === 0) {
      return { completed: 0, total: 0, percentage: 0 };
    }

    // ✅ USAR requestedBags en lugar de totalBags
    const total = this.roadmap.generators.reduce((sum, gen) => sum + (gen.requestedBags || gen.totalBags || 0), 0);
    const completed = this.roadmap.generators.reduce((sum, gen) => sum + (gen.collectedBags || 0), 0);
    const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;
    return { completed, total, percentage };
  }

  /**
   * ✅ Obtener estado del generador basado en requestedBags vs collectedBags
   */
  getGeneratorStatus(generator: GeneratorInRoadmap): string {
    const requestedBags = generator.requestedBags || generator.totalBags || 0;
    const collectedBags = generator.collectedBags || 0;

    if (collectedBags === 0) return 'PENDIENTE';
    if (collectedBags >= requestedBags) return 'COMPLETADO';
    return 'EN_PROCESO';
  }

  /**
   * ✅ Verificar si la hoja de ruta está completa basado en órdenes
   */
  isRoadmapComplete(): boolean {
    if (!this.roadmap?.generators || this.roadmap.generators.length === 0) return false;

    return this.roadmap.generators.every(gen => {
      const requestedBags = gen.requestedBags || gen.totalBags || 0;
      const collectedBags = gen.collectedBags || 0;
      return collectedBags >= requestedBags;
    });
  }

  /**
   * ✅ Obtener cantidad de bolsas solicitadas en la orden
   */
  getRequestedBagsCount(generator: GeneratorInRoadmap): number {
    return generator.requestedBags || generator.totalBags || 0;
  }

  /**
   * ✅ Obtener cantidad de bolsas recolectadas
   */
  getCollectedBagsCount(generator: GeneratorInRoadmap): number {
    return generator.collectedBags || 0;
  }

  /**
   * ✅ Obtener cantidad de bolsas pendientes
   */
  getPendingBagsCount(generator: GeneratorInRoadmap): number {
    const requested = this.getRequestedBagsCount(generator);
    const collected = this.getCollectedBagsCount(generator);
    return Math.max(0, requested - collected);
  }

  /**
   * ✅ Obtener conteo de generadores pendientes
   */
  getPendingGeneratorsCount(): number {
    if (!this.roadmap?.generators) return 0;
    return this.roadmap.generators.filter(g =>
      this.getGeneratorStatus(g) === 'PENDIENTE'
    ).length;
  }

  /**
   * ✅ MÉTODOS HELPER PARA EL TEMPLATE
   */

  /**
   * Calcular porcentaje de progreso de un generador
   */
  getProgressPercentage(generator: GeneratorInRoadmap): number {
    const requested = generator.requestedBags || generator.totalBags || 0;
    const collected = generator.collectedBags || 0;
    return requested > 0 ? Math.round((collected / requested) * 100) : 0;
  }

  /**
   * Obtener cantidad de precintos ocupados (sin recolectar)
   */
  getOccupiedSealsCount(generator: GeneratorInRoadmap): number {
    return generator.seals ? generator.seals.filter(seal => seal.state === 'OCUPADO').length : 0;
  }

  /**
   * Obtener cantidad de precintos recolectados
   */
  getCollectedSealsCount(generator: GeneratorInRoadmap): number {
    return generator.seals ? generator.seals.filter(seal => seal.state === 'RECOLECTADO').length : 0;
  }

  /**
   * ✅ CARGAR HOJA DE RUTA ACTUALIZADA
   */
  loadRoadmap(): void {
    this.loading = true;
    this.error = '';

    this.empleadoService.getSpecificRoadmap(this.roadmapId).subscribe({
      next: (roadmap) => {
        this.roadmap = roadmap;
        this.loading = false;

        // 🆕 Verificar soporte de optimización después de cargar
        this.checkRouteOptimizationSupport();

        console.log('📋 Roadmap cargado:', roadmap);
        console.log('📦 Generadores con requestedBags:', roadmap.generators);

        setTimeout(() => this.updateProgressCircle(), 100);
      },
      error: (error) => {
        console.error('Error loading roadmap:', error);
        this.error = 'Error al cargar la hoja de ruta';
        this.loading = false;
      }
    });
  }

  /**
   * ✅ ABRIR ESCÁNER QR ACTUALIZADO
   */
  openQrScanner(generator: GeneratorInRoadmap): void {
    const dialogRef = this.dialog.open(QrScannerComponent, {
      width: '90vw',
      maxWidth: '500px',
      height: '70vh',
      data: {
        generator: generator,
        roadmapId: this.roadmapId,
        // ✅ Pasar información de la orden
        requestedBags: generator.requestedBags || generator.totalBags,
        orderId: generator.orderId || null
      },
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && result.success) {
        // ✅ Recargar la hoja de ruta para obtener datos actualizados
        this.loadRoadmap();

        this.showSuccessMessage(result);

        // 🆕 Actualizar optimización después de escanear
        if (this.routeSummary) {
          this.loadOptimizedRouteSummary();
        }
        this.updateRouteOptimization();
      }
    });
  }

  // ===================================================
  // MÉTODOS DE OPTIMIZACIÓN ACTUALIZADOS
  // ===================================================

  /**
   * ✅ Preparar waypoints para optimización (actualizado)
   */
  private prepareWaypointsForOptimization(): void {
    if (!this.roadmap?.generators) return;

    this.optimizedWaypoints = this.roadmap.generators
      .filter(g => this.hasValidCoordinates(g) && this.getGeneratorStatus(g) === 'PENDIENTE')
      .map((gen, index) => ({
        id: gen.generatorId,
        name: gen.name,
        latitude: gen.latitude!,
        longitude: gen.longitude!,
        order: index + 1
      }));
  }

  /**
   * ✅ Verificar si la ruta soporta optimización (actualizado)
   */
  private checkRouteOptimizationSupport(): void {
    if (!this.roadmap?.generators) return;

    const generatorsWithCoords = this.roadmap.generators.filter(g =>
      this.hasValidCoordinates(g)
    );

    this.routeOptimizationSupported = generatorsWithCoords.length > 1;

    if (this.routeOptimizationSupported) {
      this.prepareWaypointsForOptimization();
    }
  }

  /**
   * ✅ Actualizar generador más cercano (actualizado)
   */
  private updateNearestGenerator(): void {
    if (!this.currentLocation || !this.roadmap?.generators) return;

    const pendingGenerators = this.roadmap.generators.filter(g =>
      this.getGeneratorStatus(g) === 'PENDIENTE' && this.hasValidCoordinates(g)
    );

    if (pendingGenerators.length === 0) {
      this.nearestGenerator = null;
      return;
    }

    let nearest = pendingGenerators[0];
    let minDistance = this.calculateDistance(
      this.currentLocation.latitude,
      this.currentLocation.longitude,
      nearest.latitude!,
      nearest.longitude!
    );

    // Encontrar el más cercano
    for (const generator of pendingGenerators) {
      const distance = this.calculateDistance(
        this.currentLocation.latitude,
        this.currentLocation.longitude,
        generator.latitude!,
        generator.longitude!
      );

      if (distance < minDistance) {
        minDistance = distance;
        nearest = generator;
      }
    }

    // Actualizar información del más cercano
    this.nearestGenerator = {
      name: nearest.name,
      distance: this.formatDistance(minDistance),
      estimatedTime: this.formatTime(Math.ceil((minDistance / 40) * 60)) // 40 km/h promedio
    };
  }

  // ===================================================
  // MÉTODOS DE UBICACIÓN (SIN CAMBIOS)
  // ===================================================

  /**
   * Verificar permisos de ubicación antes de intentar obtenerla
   */
  private async checkLocationPermissions(): Promise<void> {
    try {
      this.locationPermissionStatus = await this.mapService.checkLocationPermission();
      console.log('📍 Estado de permisos:', this.locationPermissionStatus);

      if (this.locationPermissionStatus === 'denied') {
        this.showLocationPermissionDialog();
      } else if (this.locationPermissionStatus === 'unsupported') {
        this.showLocationUnsupportedDialog();
      }
    } catch (error) {
      console.error('Error verificando permisos:', error);
    }
  }

  /**
   * Mostrar diálogo para permisos denegados
   */
  private showLocationPermissionDialog(): void {
    this.showMessage(
      '📍 Para una mejor experiencia, habilita los permisos de ubicación en tu navegador',
      'info',
      8000
    );
  }

  /**
   * Mostrar diálogo para navegadores no compatibles
   */
  private showLocationUnsupportedDialog(): void {
    this.showMessage(
      '⚠️ Tu navegador no soporta geolocalización. Se usará ubicación predeterminada.',
      'warn',
      8000
    );
  }

  /**
   * Solicitar permisos de ubicación explícitamente
   */
  requestLocationPermission(): void {
    this.gettingLocation = true;

    this.mapService.requestLocationPermission().subscribe({
      next: (granted) => {
        if (granted) {
          this.refreshLocation();
        } else {
          this.gettingLocation = false;
          this.showMessage(
            '❌ Permisos de ubicación denegados. Se usará ubicación predeterminada.',
            'warn'
          );
          this.useLocationFallback();
        }
      },
      error: (error) => {
        this.gettingLocation = false;
        console.error('Error requesting permissions:', error);
        this.useLocationFallback();
      }
    });
  }

  /**
   * Actualizar ubicación del empleado con manejo de errores mejorado
   */
  refreshLocation(): void {
    this.gettingLocation = true;
    this.locationErrorInfo = null;

    this.mapService.getCurrentLocation().subscribe({
      next: (location) => {
        this.currentLocation = location;
        this.usesFallbackLocation = false;
        this.gettingLocation = false;

        // Actualizar ubicación en el backend
        this.empleadoService.updateEmployeeLocation(
          location.latitude,
          location.longitude,
          'WORKING'
        ).subscribe({
          next: () => console.log('✅ Ubicación actualizada en servidor'),
          error: (error) => console.warn('⚠️ Error actualizando ubicación en servidor:', error)
        });

        // 🆕 Calcular generador más cercano y rutas mejoradas
        this.updateNearestGenerator();
        this.updateRouteOptimization();

        const accuracyText = location.accuracy ? ` (±${Math.round(location.accuracy)}m)` : '';
        this.showMessage(`📍 Ubicación actualizada correctamente${accuracyText}`, 'success');
      },
      error: (error: LocationError) => {
        this.handleLocationError(error);
      }
    });
  }

  /**
   * Inicializar seguimiento de ubicación con manejo de errores
   */
  private initializeLocationTracking(): void {
    // Primero intentar obtener ubicación inicial
    this.mapService.getCurrentLocationWithFallback().subscribe({
      next: (location) => {
        this.currentLocation = location;

        // Verificar si es la ubicación de respaldo
        const fallback = this.mapService.getFallbackLocation();
        this.usesFallbackLocation = (
          location.latitude === fallback.latitude &&
          location.longitude === fallback.longitude
        );

        if (this.usesFallbackLocation) {
          this.showMessage(
            '📍 Usando ubicación predeterminada (Córdoba). Toca "Actualizar Ubicación" para usar GPS.',
            'info',
            5000
          );
        } else {
          this.showMessage(
            '📍 Ubicación GPS obtenida correctamente',
            'success',
            3000
          );
        }

        this.updateNearestGenerator();
        this.updateRouteOptimization();
      },
      error: (error) => {
        console.error('❌ Error getting initial location:', error);
        this.useLocationFallback();
      }
    });

    // Iniciar seguimiento en segundo plano si es compatible
    if (this.mapService.isGeolocationSupported() && this.locationPermissionStatus === 'granted') {
      this.locationSubscription = this.mapService.startLocationTracking(false).subscribe({
        next: (location) => {
          if (location) {
            this.currentLocation = location;
            this.usesFallbackLocation = false;
            this.updateNearestGenerator();
            this.updateRouteOptimization();
          }
        },
        error: (error: LocationError) => {
          if (!this.locationErrorInfo) {
            this.locationErrorInfo = error;
          }
        }
      });
    }
  }

  /**
   * Manejar errores de ubicación de forma user-friendly
   */
  private handleLocationError(error: LocationError): void {
    this.gettingLocation = false;
    this.locationErrorInfo = error;

    console.error('❌ Error de ubicación:', error);

    let message = '';
    let actionButton = '';

    switch (error.type) {
      case LocationErrorType.PERMISSION_DENIED:
        message = '🚫 Permisos de ubicación denegados. Habilítalos para una mejor experiencia.';
        actionButton = 'Cómo habilitar permisos';
        break;

      case LocationErrorType.POSITION_UNAVAILABLE:
        message = '📡 No se puede determinar tu ubicación. Verifica tu conexión o intenta en el exterior.';
        actionButton = 'Usar ubicación manual';
        break;

      case LocationErrorType.TIMEOUT:
        message = '⏰ Tiempo agotado obteniendo ubicación. ¿Intentar nuevamente?';
        actionButton = 'Reintentar';
        break;

      case LocationErrorType.NOT_SUPPORTED:
        message = '📱 Tu dispositivo no soporta geolocalización.';
        actionButton = 'Usar ubicación predeterminada';
        break;

      default:
        message = `❌ ${error.message}`;
        actionButton = error.canRetry ? 'Reintentar' : 'Continuar';
        break;
    }

    // Mostrar snackbar con acción
    const snackBarRef = this.snackBar.open(message, actionButton, {
      duration: error.canRetry ? 8000 : 5000,
      panelClass: ['error-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });

    snackBarRef.onAction().subscribe(() => {
      this.handleLocationErrorAction(error);
    });

    // Usar ubicación de respaldo
    this.useLocationFallback();
  }

  /**
   * Manejar acciones de errores de ubicación
   */
  private handleLocationErrorAction(error: LocationError): void {
    switch (error.type) {
      case LocationErrorType.PERMISSION_DENIED:
        this.showLocationPermissionInstructions();
        break;

      case LocationErrorType.POSITION_UNAVAILABLE:
        this.showManualLocationDialog();
        break;

      case LocationErrorType.TIMEOUT:
        this.refreshLocation();
        break;

      default:
        if (error.canRetry) {
          this.refreshLocation();
        } else {
          this.useLocationFallback();
        }
        break;
    }
  }

  /**
   * Usar ubicación de respaldo
   */
  private useLocationFallback(): void {
    const fallbackLocation = this.mapService.getFallbackLocation();
    this.currentLocation = fallbackLocation;
    this.usesFallbackLocation = true;
    this.updateNearestGenerator();
    this.updateRouteOptimization();

    this.showMessage(
      '📍 Usando ubicación predeterminada (Córdoba). Puedes obtener tu ubicación GPS más tarde.',
      'info',
      4000
    );
  }

  /**
   * Mostrar instrucciones para habilitar permisos
   */
  private showLocationPermissionInstructions(): void {
    const instructions = `
    Para habilitar la geolocalización:

    🔹 Chrome/Edge: Haz clic en el icono 🔒 junto a la URL
    🔹 Firefox: Haz clic en el icono 🛡️ en la barra de direcciones
    🔹 Safari: Ve a Configuración > Privacidad > Servicios de ubicación

    Luego selecciona "Permitir" para este sitio.
    `;

    this.showMessage(instructions, 'info', 12000);
  }

  /**
   * Mostrar diálogo para ubicación manual
   */
  private showManualLocationDialog(): void {
    this.showMessage(
      '📍 Puedes continuar con la ubicación predeterminada o intentar nuevamente cuando tengas mejor señal.',
      'info',
      6000
    );
  }

  /**
   * Actualizar optimización de rutas cuando cambie la ubicación
   */
  private updateRouteOptimization(): void {
    if (!this.currentLocation || !this.routeOptimizationSupported) return;

    // Actualizar waypoints con generadores pendientes
    this.prepareWaypointsForOptimization();

    // Si tenemos múltiples destinos, calcular ruta optimizada
    if (this.optimizedWaypoints.length > 1) {
      const start: [number, number] = [this.currentLocation.latitude, this.currentLocation.longitude];

      this.routeService.getOptimizedMultiStopRoute(start, this.optimizedWaypoints, false).subscribe({
        next: (optimizedRoute) => {
          this.optimizedMultiStopRoute = optimizedRoute;
          console.log('🔄 Ruta multi-parada optimizada:', optimizedRoute);
        },
        error: (error) => {
          console.warn('⚠️ Error optimizando ruta multi-parada:', error);
        }
      });
    }
  }

  /**
   * Abrir vista de mapa mejorada para un generador específico
   */
  openEnhancedMapView(generator: GeneratorInRoadmap): void {
    if (!this.hasValidCoordinates(generator)) {
      this.showMessage('Este generador no tiene coordenadas válidas', 'warn');
      return;
    }

    if (!this.currentLocation) {
      this.showMessage('Obteniendo tu ubicación...', 'info');
      this.refreshLocation();
      return;
    }

    const dialogData: EnhancedMapDialogData = {
      generator: {
        id: generator.generatorId,
        name: generator.name,
        address: generator.address || 'Dirección no disponible',
        latitude: generator.latitude!,
        longitude: generator.longitude!,
        status: this.getGeneratorStatus(generator)
      },
      allGenerators: this.routeOptimizationSupported ? this.optimizedWaypoints : undefined,
      showOptimizedRoute: this.showOptimizedRoutes && this.routeOptimizationSupported
    };

    // Usar el mapa mejorado si está disponible, sino el original
    const dialogRef = this.dialog.open(MapViewComponent, {
      data: dialogData,
      maxWidth: '95vw',
      maxHeight: '95vh',
      panelClass: 'enhanced-map-dialog-panel',
      disableClose: false
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log('Mapa cerrado');
    });
  }

  /**
   * Alternar entre vista de rutas optimizadas y normales
   */
  toggleOptimizedRoutes(): void {
    this.showOptimizedRoutes = !this.showOptimizedRoutes;

    if (this.showOptimizedRoutes && !this.routeOptimizationSupported) {
      this.showMessage(
        'Se necesitan múltiples generadores con coordenadas para optimizar rutas',
        'info'
      );
      this.showOptimizedRoutes = false;
      return;
    }

    const status = this.showOptimizedRoutes ? 'activadas' : 'desactivadas';
    this.showMessage(`🔄 Rutas optimizadas ${status}`, 'info');
  }

  /**
   * Mostrar ruta optimizada completa
   */
  showCompleteOptimizedRoute(): void {
    if (!this.currentLocation) {
      this.showMessage('Primero necesitamos tu ubicación para calcular la ruta', 'info');
      this.refreshLocation();
      return;
    }

    if (!this.routeOptimizationSupported || this.optimizedWaypoints.length <= 1) {
      this.showMessage(
        'Se necesitan múltiples destinos para mostrar ruta optimizada',
        'warn'
      );
      return;
    }

    this.loadingRoute = true;

    const start: [number, number] = [this.currentLocation.latitude, this.currentLocation.longitude];

    this.routeService.getOptimizedMultiStopRoute(start, this.optimizedWaypoints, false).subscribe({
      next: (optimizedRoute) => {
        this.optimizedMultiStopRoute = optimizedRoute;
        this.loadingRoute = false;

        // Abrir en Google Maps
        const url = this.routeService.generateGoogleMapsUrlForOptimizedRoute(optimizedRoute);
        window.open(url, '_blank');

        const locationNote = this.usesFallbackLocation ?
          ' (usando ubicación aproximada)' : '';

        this.showMessage(`🗺️ Ruta optimizada abierta en Google Maps${locationNote}`, 'success');
      },
      error: (error) => {
        this.loadingRoute = false;
        console.error('Error loading optimized route:', error);
        this.showMessage('Error al cargar la ruta optimizada', 'error');
      }
    });
  }

  // ===================================================
  // MÉTODOS EXISTENTES ACTUALIZADOS
  // ===================================================

  /**
   * Abrir modal de mapa para un generador específico (método original mantenido para compatibilidad)
   */
  openMapView(generator: GeneratorInRoadmap): void {
    // Por ahora, redirigir al mapa mejorado
    this.openEnhancedMapView(generator);
  }

  /**
   * Mostrar ruta optimizada completa (actualizado)
   */
  showOptimizedRoute(): void {
    // Verificar ubicación antes de calcular ruta
    if (!this.currentLocation) {
      this.showMessage('Primero necesitamos tu ubicación para calcular la ruta', 'info');
      this.refreshLocation();
      return;
    }

    this.loadingRoute = true;

    this.routeService.getOptimizedRoute(this.roadmapId).subscribe({
      next: (route) => {
        this.optimizedRoute = route;
        this.routeSummary = route.summary;
        this.loadingRoute = false;

        // 🆕 También preparar ruta mejorada si es posible
        if (route.waypoints && route.waypoints.length > 0) {
          const simpleWaypoints = this.routeService.convertToSimpleWaypoints(route.waypoints);
          const start: [number, number] = [this.currentLocation!.latitude, this.currentLocation!.longitude];

          this.routeService.getOptimizedMultiStopRoute(start, simpleWaypoints, false).subscribe({
            next: (enhancedRoute) => {
              this.optimizedMultiStopRoute = enhancedRoute;
            },
            error: (error) => {
              console.warn('No se pudo crear ruta mejorada:', error);
            }
          });
        }

        const locationNote = this.usesFallbackLocation ?
          ' (usando ubicación aproximada)' : '';

        this.showMessage(`🗺️ Ruta optimizada cargada${locationNote}`, 'success');
      },
      error: (error) => {
        this.loadingRoute = false;
        console.error('Error loading optimized route:', error);
        this.showMessage('Error al cargar la ruta optimizada', 'error');
      }
    });
  }

  /**
   * Obtener tooltip para el botón de mapa (actualizado)
   */
  getMapButtonTooltip(generator: GeneratorInRoadmap): string {
    if (this.getGeneratorStatus(generator) === 'COMPLETADO') {
      return 'Generador ya completado';
    }
    if (!this.hasValidCoordinates(generator)) {
      return 'No hay coordenadas disponibles para este generador';
    }
    if (!this.currentLocation) {
      return 'Obteniendo tu ubicación...';
    }

    const routeType = this.routeOptimizationSupported ? 'optimizada' : 'directa';
    return `Ver ubicación y ruta ${routeType} en el mapa`;
  }

  /**
   * Verificar si un generador tiene coordenadas válidas
   */
  hasValidCoordinates(generator: GeneratorInRoadmap): boolean {
    return !!(generator.latitude && generator.longitude &&
      generator.latitude !== 0 && generator.longitude !== 0);
  }

  // ===================================================
  // MÉTODOS EXISTENTES SIN CAMBIOS
  // ===================================================

  loadEmployeeInfo(): void {
    this.empleadoService.getCurrentEmployeeInfo().subscribe({
      next: (info) => {
        this.employeeName = info.name;
      },
      error: (error) => {
        console.error('Error loading employee info:', error);
      }
    });
  }

  showSuccessMessage(scanResult: ScanQRResponse): void {
    this.snackBar.open(
      `✅ ${scanResult.message} - ${scanResult.sealNumber}`,
      'Cerrar',
      {
        duration: 5000,
        panelClass: ['success-snackbar'],
        horizontalPosition: 'center',
        verticalPosition: 'top'
      }
    );
  }

  getGeneratorStatusClass(status: string): string {
    switch (status) {
      case 'PENDIENTE': return 'status-pending';
      case 'EN_PROCESO': return 'status-in-progress';
      case 'COMPLETADO': return 'status-completed';
      default: return '';
    }
  }

  getGeneratorStatusIcon(status: string): string {
    switch (status) {
      case 'PENDIENTE': return 'schedule';
      case 'EN_PROCESO': return 'sync';
      case 'COMPLETADO': return 'check_circle';
      default: return 'help';
    }
  }

  goBack(): void {
    this.router.navigate(['/empleado']);
  }

  refresh(): void {
    this.loadRoadmap();
    if (this.routeSummary) {
      this.loadOptimizedRouteSummary();
    }
    // 🆕 También actualizar optimización
    this.updateRouteOptimization();
  }

  updateProgressCircle(): void {
    if (this.roadmap) {
      const progress = this.getTotalProgress();
      const progressCircle = document.querySelector('.progress-circle') as HTMLElement;
      if (progressCircle) {
        progressCircle.style.setProperty('--progress', `${progress.percentage}%`);
      }
    }
  }

  // ===================================================
  // MÉTODOS UTILITARIOS
  // ===================================================

  private calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    return this.routeService.calculateDistance(lat1, lon1, lat2, lon2);
  }

  private formatDistance(distanceKm: number): string {
    return this.routeService.formatDistance(distanceKm * 1000, true); // Convertir a metros
  }

  formatTime(minutes: number): string {
    return this.routeService.formatTime(minutes);
  }

  /**
   * Mostrar mensaje al usuario con duración personalizable
   */
  private showMessage(message: string, type: 'success' | 'warn' | 'error' | 'info', duration: number = 4000): void {
    const panelClass = [`${type}-snackbar`];
    this.snackBar.open(message, 'Cerrar', {
      duration: duration,
      panelClass: panelClass,
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

  // ===================================================
  // MÉTODOS ADICIONALES PARA COMPATIBILIDAD
  // ===================================================

  /**
   * Método de compatibilidad para otros métodos existentes
   */
  private loadOptimizedRouteSummary(): void {
    this.routeService.getOptimizedRoute(this.roadmapId).subscribe({
      next: (route) => {
        this.optimizedRoute = route;
        this.routeSummary = route.summary;
        this.updateNearestGenerator();
      },
      error: (error) => {
        console.error('Error loading route summary:', error);
      }
    });
  }
}
