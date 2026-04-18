// qr-scanner.component.ts - VERSIÓN ACTUALIZADA CON MANEJO DE ERRORES MEJORADO
import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef} from '@angular/material/dialog';
import { BarcodeFormat } from '@zxing/library';
import {GeneratorInRoadmap, ScanQRResponse} from "../../models/Interfaces";
import {EmpleadoService} from "../../services/Empleado/empleado.service";
import {MatIcon} from "@angular/material/icon";
import {MatProgressBar} from "@angular/material/progress-bar";
import {MatDivider} from "@angular/material/divider";
import {MatButtonToggle, MatButtonToggleGroup} from "@angular/material/button-toggle";
import {ZXingScannerModule} from "@zxing/ngx-scanner";
import {NgIf} from "@angular/common";
import {MatButton, MatIconButton} from "@angular/material/button";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {MatFormField, MatHint, MatLabel} from "@angular/material/form-field";
import {FormsModule} from "@angular/forms";
import {MatInput} from "@angular/material/input";
import {MatList, MatListOption} from "@angular/material/list";

// ✅ Interfaz para manejo de errores de cámara
interface CameraError {
  type: 'permission' | 'device' | 'network' | 'hardware' | 'unknown';
  message: string;
  canRetry: boolean;
  useManual: boolean;
  details?: string;
}

@Component({
  selector: 'app-qr-scanner',
  templateUrl: './qr-scanner.component.html',
  standalone: true,
  imports: [
    MatIcon,
    MatProgressBar,
    MatDivider,
    MatDialogContent,
    MatButtonToggleGroup,
    MatButtonToggle,
    ZXingScannerModule,
    NgIf,
    MatButton,
    MatProgressSpinner,
    MatIconButton,
    MatFormField,
    FormsModule,
    MatDialogActions,
    MatInput,MatHint, MatList, MatListOption,MatLabel
  ],
  styleUrls: ['./qr-scanner.component.css']
})
export class QrScannerComponent implements OnInit, OnDestroy {

  // ✅ Variables mejoradas para el scanner
  scannerEnabled = false; // Iniciar deshabilitado hasta verificar permisos
  scanning = false;
  scanResult: string = '';
  error = '';
  manualInput = '';
  useManualInput = false;

  // ✅ Variables para manejo de cámara mejorado
  torchEnabled = false;
  torchAvailable = false;
  hasDevices = false;
  hasPermission = false;
  cameraInitialized = false;
  cameraError: CameraError | null = null;

  // ✅ Variables para reintentos y estados
  maxRetries = 3;
  currentRetries = 0;
  initializingCamera = false;
  cameraInitAttempts = 0;
  maxInitAttempts = 2;

  // Formatos de código de barras permitidos
  allowedFormats = [BarcodeFormat.QR_CODE];

  // ✅ Timer para cleanup
  private initTimer: any;
  private retryTimer: any;

  constructor(
    public dialogRef: MatDialogRef<QrScannerComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {
      generator: GeneratorInRoadmap;
      roadmapId: number;
      requestedBags?: number;
      orderId?: number;
    },
    private empleadoService: EmpleadoService
  ) {}

  ngOnInit(): void {
    console.log('🎥 Inicializando QR Scanner...');

    // ✅ Verificar soporte de getUserMedia primero
    if (!this.isMediaSupported()) {
      this.handleUnsupportedDevice();
      return;
    }

    // ✅ Pequeño delay para permitir que el DOM se renderice
    this.initTimer = setTimeout(() => {
      this.initializeCamera();
    }, 500);
  }

  ngOnDestroy(): void {
    console.log('🔄 Destruyendo QR Scanner...');
    this.cleanupResources();
  }

  // ✅ MÉTODOS DE INICIALIZACIÓN MEJORADOS

  /**
   * Verificar si getUserMedia está soportado
   */
  private isMediaSupported(): boolean {
    const isSupported = !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);
    console.log('📱 MediaDevices soportado:', isSupported);
    return isSupported;
  }

  /**
   * Manejar dispositivos no compatibles
   */
  private handleUnsupportedDevice(): void {
    console.warn('❌ Dispositivo no soporta getUserMedia');
    this.cameraError = {
      type: 'device',
      message: 'Tu dispositivo no soporta acceso a cámara',
      canRetry: false,
      useManual: true,
      details: 'El navegador no tiene soporte para MediaDevices API'
    };
    this.useManualInput = true;
    this.error = 'Dispositivo no compatible. Usa entrada manual.';
  }

  /**
   * Inicializar cámara con manejo de errores robusto
   */
  private async initializeCamera(): Promise<void> {
    if (this.cameraInitAttempts >= this.maxInitAttempts) {
      console.warn('🚫 Máximo de intentos de inicialización alcanzado');
      this.fallbackToManual('Múltiples fallos de inicialización');
      return;
    }

    this.cameraInitAttempts++;
    this.initializingCamera = true;
    this.error = '';
    this.cameraError = null;

    console.log(`🔄 Intento de inicialización ${this.cameraInitAttempts}/${this.maxInitAttempts}`);

    try {
      // ✅ Verificar permisos primero
      const permissionStatus = await this.checkCameraPermissions();

      if (permissionStatus === 'denied') {
        this.handlePermissionDenied();
        return;
      }

      // ✅ Verificar dispositivos disponibles
      const devices = await this.getAvailableDevices();

      if (devices.length === 0) {
        this.handleNoDevices();
        return;
      }

      // ✅ Todo OK, habilitar scanner
      this.enableScanner();

    } catch (error) {
      console.error('❌ Error inicializando cámara:', error);
      this.handleCameraInitError(error);
    }
  }

  /**
   * Verificar permisos de cámara
   */
  private async checkCameraPermissions(): Promise<string> {
    try {
      const permission = await navigator.permissions.query({ name: 'camera' as PermissionName });
      console.log('🔐 Estado de permisos:', permission.state);
      return permission.state;
    } catch (error) {
      console.warn('⚠️ No se pudo verificar permisos:', error);
      return 'prompt'; // Asumir que se puede solicitar
    }
  }

  /**
   * Obtener dispositivos de cámara disponibles
   */
  private async getAvailableDevices(): Promise<MediaDeviceInfo[]> {
    try {
      const devices = await navigator.mediaDevices.enumerateDevices();
      const videoDevices = devices.filter(device => device.kind === 'videoinput');
      console.log(`📹 Dispositivos de video encontrados: ${videoDevices.length}`);
      return videoDevices;
    } catch (error) {
      console.error('❌ Error enumerando dispositivos:', error);
      throw error;
    }
  }

  /**
   * Habilitar scanner después de verificaciones
   */
  private enableScanner(): void {
    console.log('✅ Habilitando scanner...');
    this.initializingCamera = false;
    this.scannerEnabled = true;
    this.cameraInitialized = true;
    this.hasPermission = true;
  }

  // ✅ MANEJADORES DE ERROR ESPECÍFICOS

  /**
   * Manejar permisos denegados
   */
  private handlePermissionDenied(): void {
    console.warn('🚫 Permisos de cámara denegados');
    this.cameraError = {
      type: 'permission',
      message: 'Permisos de cámara denegados',
      canRetry: true,
      useManual: true,
      details: 'Habilita los permisos de cámara en tu navegador'
    };
    this.initializingCamera = false;
    this.error = 'Permisos denegados. Habilita la cámara o usa entrada manual.';
    this.hasPermission = false;
  }

  /**
   * Manejar ausencia de dispositivos
   */
  private handleNoDevices(): void {
    console.warn('📱 No hay dispositivos de cámara disponibles');
    this.cameraError = {
      type: 'device',
      message: 'No se encontraron cámaras',
      canRetry: false,
      useManual: true,
      details: 'No hay dispositivos de cámara conectados'
    };
    this.initializingCamera = false;
    this.error = 'No hay cámaras disponibles. Usa entrada manual.';
    this.hasDevices = false;
  }

  /**
   * Manejar errores de inicialización
   */
  private handleCameraInitError(error: any): void {
    console.error('💥 Error de inicialización:', error);

    let cameraError: CameraError;
    const errorName = error.name || '';
    const errorMessage = error.message || error.toString();

    switch (errorName) {
      case 'NotReadableError':
        cameraError = {
          type: 'hardware',
          message: 'Cámara en uso por otra aplicación',
          canRetry: true,
          useManual: true,
          details: 'Cierra otras aplicaciones que usen la cámara'
        };
        break;

      case 'NotAllowedError':
        cameraError = {
          type: 'permission',
          message: 'Permisos de cámara denegados',
          canRetry: true,
          useManual: true,
          details: 'Permite el acceso a la cámara en tu navegador'
        };
        break;

      case 'NotFoundError':
        cameraError = {
          type: 'device',
          message: 'No se encontró cámara',
          canRetry: false,
          useManual: true,
          details: 'No hay dispositivos de cámara disponibles'
        };
        break;

      case 'NotSupportedError':
        cameraError = {
          type: 'device',
          message: 'Cámara no soportada',
          canRetry: false,
          useManual: true,
          details: 'El navegador no soporta el acceso a cámara'
        };
        break;

      default:
        cameraError = {
          type: 'unknown',
          message: 'Error desconocido de cámara',
          canRetry: true,
          useManual: true,
          details: errorMessage
        };
        break;
    }

    this.cameraError = cameraError;
    this.initializingCamera = false;
    this.error = `${cameraError.message}. ${cameraError.useManual ? 'Usa entrada manual.' : ''}`;

    // ✅ Retry automático para algunos tipos de error
    if (cameraError.canRetry && this.cameraInitAttempts < this.maxInitAttempts) {
      console.log('🔄 Programando reintento automático...');
      this.retryTimer = setTimeout(() => {
        this.initializeCamera();
      }, 2000);
    } else {
      this.fallbackToManual(cameraError.message);
    }
  }

  /**
   * Fallback a entrada manual
   */
  private fallbackToManual(reason: string): void {
    console.log('📝 Fallback a entrada manual:', reason);
    this.useManualInput = true;
    this.scannerEnabled = false;
    this.initializingCamera = false;

    if (!this.error) {
      this.error = `${reason}. Usando entrada manual.`;
    }
  }

  // ✅ MÉTODOS DE EVENTOS DEL SCANNER (ACTUALIZADOS)

  /**
   * Cuando se detectan cámaras disponibles
   */
  onCamerasFound(devices: MediaDeviceInfo[]): void {
    console.log('📹 Cámaras encontradas:', devices.length);
    this.hasDevices = devices && devices.length > 0;

    if (!this.hasDevices) {
      this.handleNoDevices();
    }
  }

  /**
   * Cuando se obtienen permisos de cámara
   */
  onPermissionResponse(permission: boolean): void {
    console.log('🔐 Respuesta de permisos:', permission);
    this.hasPermission = permission;

    if (!permission) {
      this.handlePermissionDenied();
    } else {
      this.cameraInitialized = true;
    }
  }

  /**
   * Cuando se detecta disponibilidad de flash
   */
  onTorchCompatible(isCompatible: boolean): void {
    console.log('🔦 Flash compatible:', isCompatible);
    this.torchAvailable = isCompatible;
  }

  /**
   * Cuando se escanea exitosamente un QR
   */
  onScanSuccess(result: string): void {
    if (this.scanning) {
      console.log('⚠️ Ignorando escaneo múltiple');
      return;
    }

    // 🔧 CAMBIO: Extraer el texto correctamente del resultado
    let qrText: string;

    // Si es un string directo, usarlo
    if (typeof result === 'string') {
      qrText = result;
    }
    // Si es un objeto, extraer la propiedad correcta
    else if (result && typeof result === 'object') {
      // @ts-ignore - ZXing puede devolver objeto con diferentes propiedades
      qrText = resultString.text || resultString.data || resultString.getText?.() || String(resultString);
    }
    else {
      console.error('❌ Formato de resultado inesperado:', result);
      this.error = 'Formato de QR no reconocido';
      return;
    }

    console.log('✅ QR escaneado exitosamente:', qrText);
    console.log('🔍 Tipo de resultado:', typeof result);
    console.log('🔍 Resultado completo:', result);

    this.scanResult = qrText;
    this.scanning = true;
    this.scannerEnabled = false;
    this.error = '';
    this.processScanResult(qrText); // ✅ Enviar solo el texto
  }

  /**
   * Cuando hay error en el scanner (MEJORADO)
   */
  onScanError(error: any): void {
    console.error('❌ Error del scanner:', error);

    // ✅ No mostrar error si ya estamos procesando
    if (this.scanning) return;

    const errorName = error.name || '';
    const errorMessage = error.message || error.toString();

    // ✅ Manejar tipos específicos de error
    if (errorName === 'NotReadableError') {
      this.handleNotReadableError();
    } else if (errorName === 'NotAllowedError') {
      this.handlePermissionDenied();
    } else {
      this.error = 'Error de cámara. Usa entrada manual si persiste.';
      console.warn('Error no específico del scanner:', errorMessage);
    }
  }

  /**
   * Manejar específicamente NotReadableError
   */
  private handleNotReadableError(): void {
    console.error('💥 NotReadableError detectado');

    this.cameraError = {
      type: 'hardware',
      message: 'Cámara ocupada o no disponible',
      canRetry: true,
      useManual: true,
      details: 'La cámara está siendo usada por otra aplicación o hay un problema de hardware'
    };

    this.error = 'Cámara no disponible. Cierra otras apps que la usen o usa entrada manual.';
    this.scannerEnabled = false;

    // ✅ Sugerir entrada manual automáticamente
    if (!this.useManualInput) {
      setTimeout(() => {
        if (!this.scannerEnabled && !this.scanning) {
          console.log('🔄 Sugiriendo entrada manual automáticamente');
          this.useManualInput = true;
        }
      }, 3000);
    }
  }

  // ✅ MÉTODOS DE PROCESAMIENTO (SIN CAMBIOS MAYORES)

  /**
   * Procesar resultado del escaneo
   */
  processScanResult(qrContent: string): void {
    console.log('🔄 Procesando resultado:', qrContent);
    this.error = '';

    this.empleadoService.scanQR(qrContent, this.data.roadmapId).subscribe({
      next: (response: ScanQRResponse) => {
        console.log('✅ Respuesta del servidor:', response);

        if (response.success) {
          // Éxito - cerrar modal con resultado
          this.dialogRef.close(response);
        } else {
          this.error = response.message || 'Error al procesar el QR';
          this.resetScanner();
        }
      },
      error: (error) => {
        console.error('❌ Error procesando QR:', error);
        this.error = error.error?.message || 'Error al procesar el precinto';
        this.resetScanner();
      }
    });
  }

  /**
   * Resetear scanner después de error
   */
  resetScanner(): void {
    console.log('🔄 Reseteando scanner...');

    setTimeout(() => {
      this.scanning = false;
      this.scanResult = '';

      // ✅ Solo reactivar si la cámara estaba funcionando
      if (this.cameraInitialized && !this.cameraError) {
        this.scannerEnabled = true;
      }
    }, 2000);
  }

  // ✅ MÉTODOS DE INTERFAZ (ACTUALIZADOS)

  /**
   * Alternar entre scanner y entrada manual
   */
  toggleInputMethod(): void {
    console.log('🔄 Cambiando método de entrada...');

    this.useManualInput = !this.useManualInput;
    this.error = '';
    this.scanResult = '';
    this.manualInput = '';

    // ✅ Solo habilitar scanner si fue inicializado correctamente
    if (!this.useManualInput && this.cameraInitialized && !this.cameraError) {
      this.scannerEnabled = true;
    } else {
      this.scannerEnabled = false;
    }
  }

  /**
   * Reintentar inicialización de cámara
   */
  retryCamera(): void {
    console.log('🔄 Reintentando inicialización de cámara...');

    this.cameraInitAttempts = 0;
    this.currentRetries = 0;
    this.cameraError = null;
    this.error = '';
    this.initializeCamera();
  }

  /**
   * Procesar entrada manual
   */
  processManualInput(): void {
    if (!this.manualInput.trim()) {
      this.error = 'Ingresa el contenido del QR';
      return;
    }

    console.log('📝 Procesando entrada manual:', this.manualInput);
    this.processScanResult(this.manualInput.trim());
  }

  /**
   * Cerrar modal
   */
  close(): void {
    console.log('❌ Cerrando modal...');
    this.cleanupResources();
    this.dialogRef.close();
  }

  /**
   * Limpiar entrada manual
   */
  clearManualInput(): void {
    this.manualInput = '';
    this.error = '';
  }

  // ✅ MÉTODOS DE UTILIDAD

  /**
   * Limpiar recursos y timers
   */
  private cleanupResources(): void {
    this.scannerEnabled = false;

    if (this.initTimer) {
      clearTimeout(this.initTimer);
      this.initTimer = null;
    }

    if (this.retryTimer) {
      clearTimeout(this.retryTimer);
      this.retryTimer = null;
    }
  }

  /**
   * Obtener mensaje de error user-friendly
   */
  getCameraErrorMessage(): string {
    if (!this.cameraError) return this.error;

    return `${this.cameraError.message}. ${this.cameraError.details || ''}`;
  }

  /**
   * Verificar si se puede reintentar
   */
  canRetryCamera(): boolean {
    return this.cameraError?.canRetry === true && !this.initializingCamera;
  }

  /**
   * Verificar si se debe mostrar entrada manual
   */
  shouldShowManualInput(): boolean {
    return this.useManualInput || this.cameraError?.useManual === true;
  }
}
