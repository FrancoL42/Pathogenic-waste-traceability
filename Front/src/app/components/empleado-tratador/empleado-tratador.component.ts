import {Component, inject} from '@angular/core';
import {TreatmentService} from "../../services/Tratamiento/treatment.service";
import {MatIcon} from "@angular/material/icon";
import {DatePipe, NgForOf, NgIf} from "@angular/common";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {
  MatCard,
  MatCardActions,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle
} from "@angular/material/card";
import {MatButton, MatFabButton, MatIconButton} from "@angular/material/button";
import {MatChip, MatChipSet} from "@angular/material/chips";
import {MatCheckbox} from "@angular/material/checkbox";
import {MatProgressBar} from "@angular/material/progress-bar";
import {MatDivider} from "@angular/material/divider";
import {EmpleadoService} from "../../services/Empleado/empleado.service";
import {MatTooltip} from "@angular/material/tooltip";

@Component({
  selector: 'app-empleado-tratador',
  standalone: true,
  imports: [
    MatIcon,
    DatePipe,
    MatProgressSpinner,
    MatCard,
    MatCardContent,
    MatButton,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatChipSet,
    MatChip,
    MatCheckbox,
    NgIf,
    MatCardActions,
    MatProgressBar,
    MatDivider,
    NgForOf,
    MatTooltip,
    MatIconButton,
    MatFabButton
  ],
  templateUrl: './empleado-tratador.component.html',
  styleUrl: './empleado-tratador.component.css'
})
export class EmpleadoTratadorComponent {

  availableSeals: any[] = [];
  containers: any[] = [];
  selectedSeals: number[] = [];
  currentContainer: any = null;
  loading = false;
  error = '';
  currentDate = new Date();
  employeeName = '';
  empleadoService = inject(EmpleadoService)
  weightingMode = false;
  selectedSealsToRemove: number[] = [];
  showContainerDetails: { [key: number]: boolean } = {};
  constructor(private treatmentService: TreatmentService) {}

  ngOnInit() {
    this.refresh();
    this.loadEmployeeInfo()
  }
  toggleContainerDetails(containerId: number) {
    this.showContainerDetails[containerId] = !this.showContainerDetails[containerId];
  }
  toggleSealRemovalSelection(sealId: number) {
    const index = this.selectedSealsToRemove.indexOf(sealId);
    if (index > -1) {
      this.selectedSealsToRemove.splice(index, 1);
    } else {
      this.selectedSealsToRemove.push(sealId);
    }
  }
  removeSealsFromContainer(containerId: number) {
    if (this.selectedSealsToRemove.length === 0) {
      alert('Selecciona precintos para remover');
      return;
    }

    const removalCount = this.selectedSealsToRemove.length;

    if (confirm(`¿Remover ${removalCount} precintos del contenedor?`)) {
      console.log('🔍 Removiendo precintos:', {
        containerId,
        sealIds: this.selectedSealsToRemove
      });

      this.treatmentService.removeSealsFromContainer(containerId, this.selectedSealsToRemove)
        .subscribe({
          next: (response) => {
            console.log('✅ Precintos removidos exitosamente:', response);

            // Limpiar selección ANTES del refresh
            this.selectedSealsToRemove = [];

            // Refresh completo para asegurar sincronización
            this.refresh();

            alert(`${removalCount} precintos removidos exitosamente`);
          },
          error: (error) => {
            console.error('❌ Error removiendo precintos:', error);

            // Limpiar selección en caso de error también
            this.selectedSealsToRemove = [];

            // Manejar el error específico de respuesta de texto
            let errorMessage = 'Error desconocido';

            if (error.error) {
              // Si el error contiene "Precintos removidos exitosamente", es un falso error
              if (typeof error.error === 'string' && error.error.includes('Precintos removidos exitosamente')) {
                console.log('✅ Operación exitosa detectada en error response');
                this.refresh();
                alert(`${removalCount} precintos removidos exitosamente`);
                return;
              }

              if (typeof error.error === 'string') {
                errorMessage = error.error;
              } else if (error.error.message) {
                errorMessage = error.error.message;
              }
            } else if (error.message) {
              errorMessage = error.message;
            }

            // Solo mostrar error si realmente es un error
            alert('Error: ' + errorMessage);

            // Refresh para verificar el estado actual
            this.refresh();
          }
        });
    }
  }

  getSafeDate(dateValue: any): Date | null {
    if (!dateValue) return null;

    // Si ya es una fecha válida
    if (dateValue instanceof Date) {
      return isNaN(dateValue.getTime()) ? null : dateValue;
    }

    // Si es un string ISO
    if (typeof dateValue === 'string') {
      const date = new Date(dateValue);
      return isNaN(date.getTime()) ? null : date;
    }

    // Si es un array [year, month, day, hour, minute, second, nano]
    // Formato que envía Spring Boot: [2025, 6, 19, 1, 8, 33, 496486000]
    if (Array.isArray(dateValue) && dateValue.length >= 3) {
      try {
        const [year, month, day, hour = 0, minute = 0, second = 0, nano = 0] = dateValue;

        // month es 0-indexed en JavaScript (0 = enero, 11 = diciembre)
        // pero Spring Boot envía 1-indexed (1 = enero, 12 = diciembre)
        const jsMonth = month - 1;

        const date = new Date(year, jsMonth, day, hour, minute, second, Math.floor(nano / 1000000));
        return isNaN(date.getTime()) ? null : date;
      } catch (error) {
        console.warn('Error parsing array date:', dateValue, error);
        return null;
      }
    }

    // Si es un timestamp numérico
    if (typeof dateValue === 'number') {
      const date = new Date(dateValue);
      return isNaN(date.getTime()) ? null : date;
    }

    // Si es un objeto con propiedades de fecha
    if (typeof dateValue === 'object' && dateValue.year) {
      try {
        const date = new Date(
          dateValue.year,
          (dateValue.month || 1) - 1, // month is 0-indexed
          dateValue.day || 1,
          dateValue.hour || 0,
          dateValue.minute || 0,
          dateValue.second || 0
        );
        return isNaN(date.getTime()) ? null : date;
      } catch (error) {
        console.warn('Error parsing object date:', dateValue, error);
        return null;
      }
    }

    console.warn('Unrecognized date format:', dateValue);
    return null;
  }
  clearRemovalSelection() {
    this.selectedSealsToRemove = [];
  }
  getOpenContainersCount(): number {
    return this.containers.filter(c => c.estado === 'ABIERTO').length;
  }

  getClosedContainersCount(): number {
    return this.containers.filter(c => c.estado === 'CERRADO').length;
  }

  getTreatedContainersCount(): number {
    return this.containers.filter(c => c.estado === 'TRATADO').length;
  }

  getTotalRemovalWeight(seals: any[]): number {
    if (!seals || this.selectedSealsToRemove.length === 0) return 0;

    return this.selectedSealsToRemove
      .map(sealId => {
        const seal = seals.find(s => s.sealId === sealId);
        return seal ? seal.peso : 0;
      })
      .reduce((total, peso) => total + peso, 0);
  }
  hasPeso(seal: any): boolean {
    return seal.peso != null && seal.peso > 0;
  }
  toggleWeightingMode() {
    this.weightingMode = !this.weightingMode;
    if (!this.weightingMode) {
      this.refresh(); // Recargar datos
    }
  }
  weightSeal(seal: any) {
    const peso = prompt(`Ingrese el peso para el precinto ${seal.qrContent} (kg):`);
    if (peso && !isNaN(Number(peso)) && Number(peso) > 0) {
      this.treatmentService.weightSeal(seal.sealId, parseFloat(peso))
        .subscribe({
          next: (response) => {
            seal.peso = parseFloat(peso); // Actualizar localmente
            alert(`Precinto pesado: ${peso} kg`);
          },
          error: (error) => {
            alert('Error: ' + error.error);
          }
        });
    }
  }
  weightSelectedSeals() {
    if (this.selectedSeals.length === 0) {
      alert('Selecciona precintos para pesar');
      return;
    }

    const pesajes: any[] = [];
    let allWeighted = true;

    for (let sealId of this.selectedSeals) {
      const seal = this.availableSeals.find(s => s.sealId === sealId);
      const peso = prompt(`Peso para ${seal.qrContent} (kg):`);

      if (!peso || isNaN(Number(peso)) || Number(peso) <= 0) {
        allWeighted = false;
        break;
      }

      pesajes.push({
        sealId: sealId,
        peso: parseFloat(peso)
      });
    }

    if (allWeighted && pesajes.length > 0) {
      this.treatmentService.weightSealsLote(pesajes)
        .subscribe({
          next: (response) => {
            alert(response);
            this.refresh();
            this.selectedSeals = [];
          },
          error: (error) => {
            alert('Error: ' + error.error);
          }
        });
    }
  }
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
  refresh() {
    console.log('🔄 Iniciando refresh de datos...');

    this.loading = true;
    this.error = '';

    Promise.all([
      this.treatmentService.getAvailableSeals().toPromise(),
      this.treatmentService.getContainers().toPromise()
    ]).then(([seals, containers]) => {
      console.log('🔄 Datos recibidos:', {
        seals: seals?.length || 0,
        containers: containers?.length || 0
      });

      // Actualizar precintos disponibles
      this.availableSeals = seals || [];
      console.log('📦 Precintos disponibles actualizados:', this.availableSeals.length);

      // Convertir fechas y actualizar contenedores
      this.containers = (containers || []).map(container => ({
        ...container,
        fechaCreacion: this.getSafeDate(container.fechaCreacion),
        fechaCierre: this.getSafeDate(container.fechaCierre),
        fechaAperturaPostTratamiento: this.getSafeDate(container.fechaAperturaPostTratamiento)
      }));

      console.log('📦 Contenedores actualizados:', this.containers.length);

      // Mantener contenedor actual si sigue abierto, sino buscar otro abierto
      if (this.currentContainer) {
        const updatedContainer = this.containers.find(c =>
          c.containerId === this.currentContainer.containerId && c.estado === 'ABIERTO');

        if (updatedContainer) {
          this.currentContainer = updatedContainer;
          console.log('📦 Contenedor actual mantenido:', this.currentContainer.containerId);
        } else {
          this.currentContainer = this.containers.find(c => c.estado === 'ABIERTO') || null;
          console.log('📦 Nuevo contenedor actual:', this.currentContainer?.containerId || 'ninguno');
        }
      } else {
        this.currentContainer = this.containers.find(c => c.estado === 'ABIERTO') || null;
        console.log('📦 Contenedor actual seleccionado:', this.currentContainer?.containerId || 'ninguno');
      }

      this.loading = false;
      console.log('✅ Refresh completado exitosamente');

    }).catch(error => {
      console.error('❌ Error en refresh:', error);
      this.error = 'Error cargando datos: ' + (error.message || 'Error desconocido');
      this.loading = false;
    });
  }

  toggleSealSelection(sealId: number) {
    const index = this.selectedSeals.indexOf(sealId);
    if (index > -1) {
      this.selectedSeals.splice(index, 1);
    } else {
      this.selectedSeals.push(sealId);
    }
  }

  clearSelection() {
    this.selectedSeals = [];
  }

  getTotalSelectedWeight(): number {
    return this.selectedSeals
      .map(id => this.availableSeals.find(s => s.sealId === id)?.peso || 0)
      .reduce((total, peso) => total + peso, 0);
  }

  setCurrentContainer(container: any) {
    this.currentContainer = container;
  }

  createNewContainer() {
    const pesoMaximo = prompt('Peso máximo del contenedor (kg):');
    if (pesoMaximo && !isNaN(Number(pesoMaximo))) {
      this.treatmentService.createContainer(parseFloat(pesoMaximo))
        .subscribe(() => {
          this.refresh();
        });
    }
  }

  addSealsToContainer() {
    if (!this.currentContainer || this.selectedSeals.length === 0) {
      alert('Selecciona precintos y asegúrate de tener un contenedor abierto');
      return;
    }

    // Verificar que todos tengan peso
    const sealsSinPeso = this.selectedSeals.filter(sealId => {
      const seal = this.availableSeals.find(s => s.sealId === sealId);
      return !this.hasPeso(seal);
    });

    if (sealsSinPeso.length > 0) {
      alert('Todos los precintos deben estar pesados antes de agregarlos al contenedor');
      return;
    }

    // Mostrar indicador de carga
    const selectedCount = this.selectedSeals.length;

    this.treatmentService.addSealsToContainer(this.currentContainer.containerId, this.selectedSeals)
      .subscribe({
        next: () => {
          console.log(`✅ ${selectedCount} precintos agregados al contenedor ${this.currentContainer.containerId}`);

          // Limpiar selección ANTES del refresh
          this.selectedSeals = [];

          // Refresh completo para asegurar sincronización
          this.refresh();

          alert(`${selectedCount} precintos agregados exitosamente al contenedor`);
        },
        error: (error) => {
          console.error('❌ Error agregando precintos:', error);
          alert('Error: ' + (error.error || error.message || 'Error desconocido'));
        }
      });
  }

  closeContainer(containerId: number) {
    if (confirm('¿Cerrar contenedor para iniciar tratamiento?')) {
      this.treatmentService.closeContainer(containerId).subscribe(() => {
        this.refresh();
      });
    }
  }

  processContainerTreatment(containerId: number) {
    if (confirm('¿Confirma que el tratamiento ha finalizado? El contenedor se vaciará y estará listo para reutilizar.')) {
      this.treatmentService.processContainerTreatment(containerId).subscribe({
        next: (response) => {
          console.log('✅ Tratamiento procesado:', response);

          if (response.success) {
            this.refresh();
            alert(`Tratamiento completado. Contenedor ${response.containerId} vaciado y listo para reutilización.`);
          } else {
            alert('Error: ' + response.message);
          }
        },
        error: (error) => {
          console.error('❌ Error procesando tratamiento:', error);

          let errorMessage = 'Error desconocido';
          if (error.error?.message) {
            errorMessage = error.error.message;
          } else if (error.message) {
            errorMessage = error.message;
          }

          alert('Error: ' + errorMessage);
        }
      });
    }
  }


  getContainerStatusClass(estado: string): string {
    return `status-${estado.toLowerCase()}`;
  }

  getContainerStatusIcon(estado: string): string {
    switch (estado) {
      case 'ABIERTO': return 'lock_open';
      case 'CERRADO': return 'lock';
      // 🗑️ ELIMINAR: case 'TRATADO': return 'check_circle';
      default: return 'help';
    }
  }

  getContainerStatusIconClass(estado: string): string {
    return estado.toLowerCase();
  }

  getContainerPercentage(container: any): number {
    return Math.round((container.pesoActual / container.pesoMaximo) * 100);
  }

  logout() {
    // Implementar logout
    console.log('Logout');
  }
  isContainerAvailable(container: any): boolean {
    return container.estado === 'ABIERTO';
  }
  getContainerStatusDescription(estado: string): string {
    switch (estado) {
      case 'ABIERTO': return 'Disponible para recibir precintos';
      case 'CERRADO': return 'En proceso de tratamiento';
      default: return 'Estado desconocido';
    }
  }
}
