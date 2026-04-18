import {Component, inject} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {GeneratorService} from "../../services/Generador/generator.service";
import {CommonModule} from "@angular/common";
import {MatError, MatFormField, MatLabel} from "@angular/material/form-field";
import {MatIcon} from "@angular/material/icon";
import {MatOption} from "@angular/material/core";
import {MatSelect} from "@angular/material/select";
import {AdminService} from "../../services/Admin/admin.service";
import {Zona} from "../../models/Zona";
import {MatDialog} from "@angular/material/dialog";
import {MatSnackBar} from "@angular/material/snack-bar";
import {MapPickerComponent} from "../map-picker/map-picker.component";
import {TermsModalComponent} from "../terms-modal/terms-modal.component";

// Interfaz para ubicación del mapa
interface MapLocation {
  latitude: number;
  longitude: number;
  accuracy?: number;
}

// Interfaz para los datos del generador con coordenadas
interface GeneradorConCoordenadas {
  name: string;
  email: string;
  address: string;
  contact: string;
  type: string;
  zona: string;
  acceptTerms: boolean;
  latitude?: number;
  longitude?: number;
}

@Component({
  selector: 'app-alta-generador',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CommonModule,
    MatError,
    MatFormField,
    MatIcon,
    MatLabel,
    MatOption,
    MatSelect,
  ],
  templateUrl: './alta-generador.component.html',
  styleUrl: './alta-generador.component.css'
})
export class AltaGeneradorComponent {
  formulario!: FormGroup;
  generatorService: GeneratorService = inject(GeneratorService);
  adminService: AdminService = inject(AdminService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  // Variables para manejo de ubicación
  selectedCoordinates: MapLocation | null = null;
  locationMethod: 'gps' | 'manual' | 'fallback' | null = null;
  isLocationPickerOpen = false;
  hasTriedAutoDetection = false;

  // Variables para zonas
  zonaSeleccionada: string = '';
  zonas: Zona[] = [];

  constructor(private fb: FormBuilder) {
    this.formulario = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      address: ['', [Validators.required, Validators.minLength(5)]],
      contact: ['', [Validators.required, Validators.minLength(8)]],
      type: ['publico', Validators.required],
      zona: ['', Validators.required],
      acceptTerms: [false, Validators.requiredTrue],
      // 🆕 Campos de coordenadas en el FormGroup
      latitude: [null],
      longitude: [null]
    });
  }

  ngOnInit(): void {
    this.cargarZonas();
    this.tryAutoDetectLocation();
  }

  /**
   * 🔍 Intentar detectar ubicación automáticamente
   */
  private tryAutoDetectLocation(): void {
    if (this.hasTriedAutoDetection) return;

    this.hasTriedAutoDetection = true;
    console.log('🔍 Intentando detectar ubicación automáticamente...');

    // Verificar si el navegador soporta geolocalización
    if (!navigator.geolocation) {
      console.log('❌ Geolocalización no soportada');
      this.useFallbackLocation();
      return;
    }

    // Opciones para getCurrentPosition
    const options = {
      enableHighAccuracy: true,
      timeout: 10000, // 10 segundos
      maximumAge: 300000 // 5 minutos
    };

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const location: MapLocation = {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          accuracy: position.coords.accuracy
        };

        // Verificar si está en Córdoba
        if (this.isWithinCordoba(location.latitude, location.longitude)) {
          this.selectedCoordinates = location;
          this.locationMethod = 'gps';
          this.updateFormCoordinates(location);

          console.log('✅ Ubicación GPS detectada:', location);
          this.showMessage('📍 Ubicación GPS detectada automáticamente', 'success');
        } else {
          console.log('⚠️ Ubicación fuera de Córdoba, usando fallback');
          this.useFallbackLocation();
        }
      },
      (error) => {
        console.log('❌ Error de geolocalización:', error.message);
        this.useFallbackLocation();
      },
      options
    );
  }

  /**
   * 🏠 Usar ubicación de respaldo (Centro de Córdoba)
   */
  private useFallbackLocation(): void {
    const fallbackLocation: MapLocation = {
      latitude: -31.4201,
      longitude: -64.1888,
      accuracy: undefined
    };

    this.selectedCoordinates = fallbackLocation;
    this.locationMethod = 'fallback';
    this.updateFormCoordinates(fallbackLocation);

    console.log('🏠 Usando ubicación de respaldo:', fallbackLocation);
    this.showMessage('🏠 Usando ubicación predeterminada (Centro de Córdoba)', 'info');
  }

  /**
   * 🗺️ Abrir selector de mapa
   */
  openMapPicker(): void {
    if (this.isLocationPickerOpen) return;

    this.isLocationPickerOpen = true;
    const currentAddress = this.formulario.get('address')?.value || '';

    console.log('🗺️ Abriendo selector de mapa...');

    const dialogRef = this.dialog.open(MapPickerComponent, {
      data: {
        currentAddress: currentAddress,
        initialCoordinates: this.selectedCoordinates
      },
      width: '90vw',
      maxWidth: '700px',
      height: '80vh',
      disableClose: false,
      panelClass: 'map-picker-dialog'
    });

    dialogRef.afterClosed().subscribe(result => {
      this.isLocationPickerOpen = false;

      if (result && result.coordinates) {
        this.selectedCoordinates = result.coordinates;
        this.locationMethod = 'manual';
        this.updateFormCoordinates(result.coordinates);

        this.showMessage('🗺️ Ubicación seleccionada en el mapa', 'success');
        console.log('✅ Coordenadas del mapa:', this.selectedCoordinates);
      }
    });
  }

  /**
   * 📝 Actualizar coordenadas en el formulario
   */
  private updateFormCoordinates(location: MapLocation): void {
    this.formulario.patchValue({
      latitude: location.latitude,
      longitude: location.longitude
    });

    console.log('📝 Formulario actualizado con coordenadas:', {
      latitude: location.latitude,
      longitude: location.longitude
    });
  }

  /**
   * 🗑️ Limpiar coordenadas del formulario
   */
  clearFormCoordinates(): void {
    this.formulario.patchValue({
      latitude: null,
      longitude: null
    });
    this.selectedCoordinates = null;
    this.locationMethod = null;

    console.log('🗑️ Coordenadas limpiadas');
    this.showMessage('🗑️ Coordenadas eliminadas', 'info');
  }

  /**
   * 📊 Obtener texto del estado de ubicación
   */
  getLocationStatusText(): string {
    if (!this.selectedCoordinates) {
      return 'No se ha confirmado ubicación GPS';
    }

    switch (this.locationMethod) {
      case 'gps':
        return '🎯 Ubicación GPS confirmada';
      case 'manual':
        return '📍 Ubicación seleccionada manualmente';
      case 'fallback':
        return '🏠 Ubicación predeterminada (Centro de Córdoba)';
      default:
        return '📍 Ubicación confirmada';
    }
  }

  /**
   * 📍 Obtener detalles de las coordenadas
   */
  getLocationDetails(): string {
    if (!this.selectedCoordinates) return '';

    const lat = this.selectedCoordinates.latitude.toFixed(6);
    const lng = this.selectedCoordinates.longitude.toFixed(6);
    const accuracy = this.selectedCoordinates.accuracy ?
      ` (±${Math.round(this.selectedCoordinates.accuracy)}m)` : '';

    return `${lat}, ${lng}${accuracy}`;
  }

  /**
   * ✅ Verificar si hay coordenadas válidas
   */
  hasValidCoordinates(): boolean {
    return !!(this.selectedCoordinates &&
      typeof this.selectedCoordinates.latitude === 'number' &&
      typeof this.selectedCoordinates.longitude === 'number' &&
      !isNaN(this.selectedCoordinates.latitude) &&
      !isNaN(this.selectedCoordinates.longitude));
  }

  /**
   * 📤 Registrar generador
   */
  registrar(): void {
    if (this.formulario.valid) {
      // Preparar datos para envío
      const generadorData: GeneradorConCoordenadas = {
        name: this.formulario.value.name,
        email: this.formulario.value.email,
        address: this.formulario.value.address,
        contact: this.formulario.value.contact,
        type: this.formulario.value.type,
        zona: this.formulario.value.zona,
        acceptTerms: this.formulario.value.acceptTerms,
        latitude: this.formulario.value.latitude,
        longitude: this.formulario.value.longitude
      };

      console.log('📤 Enviando datos del generador:', generadorData);

      this.generatorService.registerClient(generadorData as any).subscribe({
        next: (response) => {
          console.log('✅ Respuesta de la API:', response);
          this.showMessage('✅ Registro exitoso con coordenadas incluidas', 'success');

          // Reset del formulario
          this.formulario.reset({
            type: 'publico',
            zona: this.zonas.length > 0 ? this.zonas[0].name : ''
          });
          this.clearFormCoordinates();
        },
        error: (err) => {
          console.error('❌ Error al registrar:', err);
          this.showMessage('❌ Ocurrió un error al registrar', 'error');
        }
      });
    } else {
      // Marcar campos como touched para mostrar errores
      Object.keys(this.formulario.controls).forEach(key => {
        this.formulario.get(key)?.markAsTouched();
      });

      this.showMessage('⚠️ Por favor complete todos los campos requeridos', 'warning');
    }
  }

  /**
   * 🌍 Cargar zonas disponibles
   */
  private cargarZonas(): void {
    this.adminService.getZonas().subscribe({
      next: (data) => {
        this.zonas = data;

        // Establecer primera zona como valor por defecto
        if (data.length > 0 && !this.formulario.get('zona')?.value) {
          this.formulario.patchValue({ zona: data[0].name });
        }

        console.log('🗺️ Zonas cargadas:', data);
      },
      error: (err) => {
        console.error('❌ Error al cargar zonas:', err);
        this.showMessage('❌ Error al cargar zonas disponibles', 'error');
      }
    });
  }

  /**
   * 🔍 Método de prueba para geolocalización
   */
  testGeolocation(): void {
    console.log('🔍 Probando geolocalización manual...');
    this.hasTriedAutoDetection = false;
    this.tryAutoDetectLocation();
  }

  /**
   * 📍 Verificar si las coordenadas están en Córdoba
   */
  private isWithinCordoba(lat: number, lng: number): boolean {
    const CORDOBA_BOUNDS = {
      north: -31.30,
      south: -31.50,
      east: -64.05,
      west: -64.30
    };

    const isInBounds = lat >= CORDOBA_BOUNDS.south &&
      lat <= CORDOBA_BOUNDS.north &&
      lng >= CORDOBA_BOUNDS.west &&
      lng <= CORDOBA_BOUNDS.east;

    console.log(`📍 Verificando coordenadas ${lat}, ${lng} en Córdoba: ${isInBounds}`);
    return isInBounds;
  }

  /**
   * 💬 Mostrar mensaje al usuario
   */
  private showMessage(message: string, type: 'success' | 'error' | 'warning' | 'info'): void {
    this.snackBar.open(message, 'Cerrar', {
      duration: type === 'success' ? 6000 : 4000,
      panelClass: [`${type}-snackbar`],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

  /**
   * 🛠️ MÉTODOS DE DEBUG (Para desarrollo)
   */

  /**
   * Obtener coordenadas actuales del formulario
   */
  getFormCoordinates(): { latitude: number | null, longitude: number | null } {
    return {
      latitude: this.formulario.get('latitude')?.value,
      longitude: this.formulario.get('longitude')?.value
    };
  }

  /**
   * Verificar si hay coordenadas válidas en el formulario
   */
  hasFormCoordinates(): boolean {
    const coords = this.getFormCoordinates();
    return coords.latitude !== null && coords.longitude !== null &&
      typeof coords.latitude === 'number' && typeof coords.longitude === 'number' &&
      !isNaN(coords.latitude) && !isNaN(coords.longitude);
  }
  // 🆕 MÉTODO PARA ABRIR EL MODAL DE TÉRMINOS Y CONDICIONES
  openTermsModal(): void {
    const dialogRef = this.dialog.open(TermsModalComponent, {
      width: '90vw',
      maxWidth: '800px',
      height: '85vh',
      disableClose: false,
      panelClass: 'terms-modal-dialog',
      autoFocus: false
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        // El usuario aceptó los términos
        this.formulario.patchValue({ acceptTerms: true });
        this.showMessage('✅ Términos y condiciones aceptados', 'success');
      }
    });
  }

  // 🆕 MÉTODO PARA ABRIR EL MODAL DE POLÍTICA DE PRIVACIDAD
  openPrivacyModal(): void {
    // Por ahora mostrar un mensaje simple, puedes crear otro modal similar
    this.showMessage('ℹ️ Modal de Política de Privacidad - Por implementar', 'info');
  }
}
