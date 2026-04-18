import { Component, OnInit, OnDestroy, inject, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MapService, MapLocation } from '../../services/map.service';

interface MapPickerData {
  currentAddress?: string;
  initialCoordinates?: MapLocation;
}

interface MapPickerResult {
  coordinates: MapLocation;
  address?: string;
}

declare var L: any;

@Component({
  selector: 'app-map-picker',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  template: `
    <div class="map-picker-container">
      <h2 mat-dialog-title class="map-title">
        <mat-icon>place</mat-icon>
        Seleccionar Ubicación
      </h2>

      <mat-dialog-content class="map-dialog-content">
        <!-- Instrucciones -->
        <div class="map-instructions">
          <mat-icon>info</mat-icon>
          <span>Haz clic en el mapa para seleccionar tu ubicación exacta</span>
        </div>

        <!-- Contenedor del mapa -->
        <div class="map-container">
          <div #mapElement id="map" class="map-element"></div>

          <!-- Overlay de carga -->
          <div class="map-loading" *ngIf="isLoadingMap">
            <mat-spinner diameter="40"></mat-spinner>
            <span>Cargando mapa...</span>
          </div>
        </div>

        <!-- Información de coordenadas seleccionadas -->
        <div class="coordinates-display" *ngIf="selectedCoordinates">
          <div class="coord-info">
            <mat-icon>my_location</mat-icon>
            <div class="coord-details">
              <strong>Coordenadas seleccionadas:</strong>
              <span class="coord-text">
                {{ selectedCoordinates.latitude | number:'1.6-6' }},
                {{ selectedCoordinates.longitude | number:'1.6-6' }}
              </span>
            </div>
          </div>

          <div class="address-info" *ngIf="data.currentAddress">
            <mat-icon>home</mat-icon>
            <span>{{ data.currentAddress }}</span>
          </div>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="map-actions">
        <button mat-button (click)="onCancel()">
          Cancelar
        </button>

        <button
          mat-button
          (click)="useMyLocation()"
          [disabled]="isGettingLocation">
          <mat-icon *ngIf="!isGettingLocation">my_location</mat-icon>
          <mat-spinner *ngIf="isGettingLocation" diameter="20"></mat-spinner>
          Mi Ubicación
        </button>

        <button
          mat-raised-button
          color="primary"
          (click)="confirmSelection()"
          [disabled]="!selectedCoordinates">
          <mat-icon>check</mat-icon>
          Confirmar
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .map-picker-container {
      width: 100%;
      max-width: 700px;
      height: 80vh;
      display: flex;
      flex-direction: column;
    }

    .map-title {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #0066CC;
      font-size: 1.3rem;
      margin: 0;
    }

    .map-dialog-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 0 24px;
      overflow: hidden;
    }

    .map-instructions {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #e3f2fd;
      padding: 12px;
      border-radius: 6px;
      margin-bottom: 16px;
      font-size: 0.9rem;
      color: #1976d2;
    }

    .map-container {
      flex: 1;
      position: relative;
      border-radius: 8px;
      overflow: hidden;
      border: 2px solid #e0e0e0;
      min-height: 400px;
    }

    .map-element {
      width: 100%;
      height: 100%;
      min-height: 400px;
    }

    .map-loading {
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
      gap: 16px;
      z-index: 1000;
    }

    .coordinates-display {
      margin-top: 16px;
      background: #f5f5f5;
      padding: 12px;
      border-radius: 6px;
      border-left: 4px solid #4caf50;
    }

    .coord-info {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;
    }

    .coord-details {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .coord-details strong {
      font-size: 0.9rem;
      color: #333;
    }

    .coord-text {
      font-family: 'Courier New', monospace;
      font-size: 0.85rem;
      color: #666;
    }

    .address-info {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.85rem;
      color: #666;
    }

    .map-actions {
      padding: 16px 24px;
      gap: 8px;
      border-top: 1px solid #e0e0e0;
    }

    @media (max-width: 768px) {
      .map-picker-container {
        max-width: 95vw;
        height: 90vh;
      }

      .map-element {
        min-height: 300px;
      }

      .map-instructions {
        font-size: 0.8rem;
        padding: 10px;
      }
    }
  `]
})
export class MapPickerComponent implements OnInit, OnDestroy {
  private mapService = inject(MapService);
  private snackBar = inject(MatSnackBar);

  selectedCoordinates: MapLocation | null = null;
  isLoadingMap = true;
  isGettingLocation = false;

  private map: any;
  private marker: any;

  // Configuración para Córdoba
  private readonly CORDOBA_CENTER = { lat: -31.4201, lng: -64.1888 };
  private readonly CORDOBA_BOUNDS = {
    north: -31.30,
    south: -31.50,
    east: -64.05,
    west: -64.30
  };

  constructor(
    private dialogRef: MatDialogRef<MapPickerComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MapPickerData
  ) {}

  ngOnInit(): void {
    // Usar coordenadas iniciales si existen, sino centro de Córdoba
    if (this.data.initialCoordinates) {
      this.selectedCoordinates = this.data.initialCoordinates;
    }

    // Inicializar mapa después de que el componente esté renderizado
    setTimeout(() => {
      this.initializeMap();
    }, 100);
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initializeMap(): void {
    try {
      // Convertir coordenadas al formato correcto
      let initialLat = this.CORDOBA_CENTER.lat;
      let initialLng = this.CORDOBA_CENTER.lng;

      // Si hay coordenadas seleccionadas, usarlas
      if (this.selectedCoordinates) {
        initialLat = this.selectedCoordinates.latitude;
        initialLng = this.selectedCoordinates.longitude;
      }

      console.log('🗺️ Inicializando mapa en:', initialLat, initialLng);

      // Crear el mapa centrado en las coordenadas
      this.map = L.map('map').setView([initialLat, initialLng], 15);

      // Agregar tiles de OpenStreetMap
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
      }).addTo(this.map);

      // Crear marcador inicial
      this.marker = L.marker([initialLat, initialLng], {
        draggable: true
      }).addTo(this.map);

      // Listener para clics en el mapa
      this.map.on('click', (e: any) => {
        this.updateMarkerPosition(e.latlng);
      });

      // Listener para arrastrar el marcador
      this.marker.on('dragend', () => {
        this.updateMarkerPosition(this.marker.getLatLng());
      });

      // Restricciones del mapa a Córdoba
      const bounds = L.latLngBounds(
        [this.CORDOBA_BOUNDS.south, this.CORDOBA_BOUNDS.west],
        [this.CORDOBA_BOUNDS.north, this.CORDOBA_BOUNDS.east]
      );
      this.map.setMaxBounds(bounds);
      this.map.setMinZoom(12);

      // Si hay coordenadas iniciales, establecerlas
      if (this.selectedCoordinates) {
        this.updateMarkerPosition({
          lat: this.selectedCoordinates.latitude,
          lng: this.selectedCoordinates.longitude
        });
      }

      this.isLoadingMap = false;
      console.log('✅ Mapa inicializado correctamente');

    } catch (error) {
      console.error('❌ Error inicializando mapa:', error);
      this.isLoadingMap = false;
      this.showMessage('Error al cargar el mapa', 'error');
    }
  }

  private updateMarkerPosition(latlng: any): void {
    const lat = latlng.lat;
    const lng = latlng.lng;

    // Verificar que esté dentro de Córdoba
    if (!this.isWithinCordoba(lat, lng)) {
      this.showMessage('⚠️ La ubicación debe estar dentro de Córdoba Capital', 'warning');
      return;
    }

    // Actualizar coordenadas en formato MapLocation
    this.selectedCoordinates = {
      latitude: lat,
      longitude: lng
    };

    // Mover marcador
    this.marker.setLatLng([lat, lng]);

    console.log('📍 Nueva ubicación seleccionada:', this.selectedCoordinates);
  }

  useMyLocation(): void {
    this.isGettingLocation = true;

    this.mapService.getCurrentLocation().subscribe({
      next: (location: MapLocation) => {
        this.isGettingLocation = false;

        if (this.isWithinCordoba(location.latitude, location.longitude)) {
          this.selectedCoordinates = location;

          // Centrar mapa y mover marcador (convertir coordenadas)
          this.map.setView([location.latitude, location.longitude], 17);
          this.marker.setLatLng([location.latitude, location.longitude]);

          this.showMessage('📍 Ubicación GPS obtenida', 'success');
        } else {
          this.showMessage('⚠️ Tu ubicación está fuera de Córdoba', 'warning');
        }
      },
      error: (error) => {
        this.isGettingLocation = false;
        console.error('Error obteniendo ubicación:', error);
        this.showMessage('❌ No se pudo obtener tu ubicación GPS', 'error');
      }
    });
  }

  confirmSelection(): void {
    if (!this.selectedCoordinates) {
      this.showMessage('❌ Selecciona una ubicación en el mapa', 'warning');
      return;
    }

    const result: MapPickerResult = {
      coordinates: this.selectedCoordinates,
      address: this.data.currentAddress
    };

    console.log('✅ Confirmando selección:', result);
    this.dialogRef.close(result);
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }

  private isWithinCordoba(lat: number, lng: number): boolean {
    return lat >= this.CORDOBA_BOUNDS.south &&
      lat <= this.CORDOBA_BOUNDS.north &&
      lng >= this.CORDOBA_BOUNDS.west &&
      lng <= this.CORDOBA_BOUNDS.east;
  }

  private showMessage(message: string, type: 'success' | 'error' | 'warning' | 'info'): void {
    this.snackBar.open(message, 'Cerrar', {
      duration: 3000,
      panelClass: [`${type}-snackbar`],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }
}
