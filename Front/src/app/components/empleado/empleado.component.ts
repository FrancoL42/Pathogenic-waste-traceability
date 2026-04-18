import { Component } from '@angular/core';
import * as L from 'leaflet';
import {FormControl, FormsModule, ReactiveFormsModule} from "@angular/forms";
import {RouteService} from "../../services/Route/route.service";
import {MatToolbar, MatToolbarModule} from "@angular/material/toolbar";
import {MatCard, MatCardContent, MatCardModule, MatCardTitle} from "@angular/material/card";
import {MatFormField, MatFormFieldModule, MatLabel} from "@angular/material/form-field";
import {MatOption, MatSelect} from "@angular/material/select";
import {MatDivider, MatDividerModule} from "@angular/material/divider";
import {MatButtonModule} from "@angular/material/button";
import {MatIconModule} from "@angular/material/icon";
import {MatDialog, MatDialogActions, MatDialogContent, MatDialogModule} from "@angular/material/dialog";
import {
  MatDatepicker,
  MatDatepickerInput,
  MatDatepickerModule,
  MatDatepickerToggle
} from "@angular/material/datepicker";
import {MatInputModule} from "@angular/material/input";
import {MatNativeDateModule} from "@angular/material/core";
import {MatTable} from "@angular/material/table";
import {MatList, MatListItem, MatListOption, MatNavList, MatSelectionList} from "@angular/material/list";
import {MatSidenav, MatSidenavContainer, MatSidenavContent} from "@angular/material/sidenav";
import {CommonModule} from "@angular/common";
import {Pedido} from "../../models/Pedido";
import {EmployeeRoadmapResponse, RoadmapCloseRequest, RoadmapCloseResponse} from "../../models/Interfaces";
import {EmpleadoService} from "../../services/Empleado/empleado.service";
import {Router} from "@angular/router";
import {MatProgressBar} from "@angular/material/progress-bar";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {MatChip, MatChipSet} from "@angular/material/chips";
import {MatSnackBar} from "@angular/material/snack-bar";
import {CloseRoadmapModalComponent} from "../close-roadmap-modal/close-roadmap-modal.component";
import {MatTooltip} from "@angular/material/tooltip";

@Component({
  selector: 'app-empleado',
  standalone: true,
  imports: [MatToolbar,
    MatCard,
    MatCardTitle,
    MatDivider,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatDividerModule,
    MatDialogContent,
    MatFormField,
    MatDatepickerToggle,
    MatDatepickerInput,
    MatDatepicker,
    MatDialogActions,
    ReactiveFormsModule,
    MatLabel,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule, MatTable, MatListItem, MatList, MatNavList, MatSidenavContainer, MatSidenav, MatSidenavContent, CommonModule, MatSelect, MatOption, MatSelectionList, FormsModule, MatListOption, MatProgressBar, MatProgressSpinner, MatChipSet, MatChip, MatTooltip],
  templateUrl: './empleado.component.html',
  styleUrl: './empleado.component.css'
})

export class EmpleadoComponent {
  roadmaps: EmployeeRoadmapResponse[] = [];
  loading = true;
  error = '';
  currentDate = new Date();
  employeeName = '';

  constructor(
    private empleadoService: EmpleadoService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadEmployeeInfo();
    this.loadRoadmaps();
    console.log('userId:', localStorage.getItem('userId'));
    console.log('token:', localStorage.getItem('token'));
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

  loadRoadmaps(): void {
    this.loading = true;
    this.error = '';

    this.empleadoService.getRoadmaps().subscribe({
      next: (roadmaps) => {
        this.roadmaps = roadmaps;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading roadmaps:', error);
        this.error = 'Sin hojas de ruta';
        this.loading = false;
      }
    });
  }

  // Navegar a trabajar una ruta específica
  workRoadmap(roadmapId: number): void {
    this.router.navigate(['/empleado/work', roadmapId]);
  }

  // Calcular progreso de la ruta
  getRoadmapProgress(roadmap: EmployeeRoadmapResponse): { completed: number, total: number, percentage: number } {
    const total = roadmap.generators.reduce((sum, gen) => sum + gen.totalBags, 0);
    const completed = roadmap.generators.reduce((sum, gen) => sum + gen.collectedBags, 0);
    const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;

    return { completed, total, percentage };
  }

  // Obtener estado de la ruta
  getRoadmapStatus(roadmap: EmployeeRoadmapResponse): string {
    const progress = this.getRoadmapProgress(roadmap);
    if (progress.completed === 0) return 'PENDIENTE';
    if (progress.completed === progress.total) return 'COMPLETADO';
    return 'EN_PROCESO';
  }

  // Obtener clase CSS según estado
  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDIENTE': return 'status-pending';
      case 'EN_PROCESO': return 'status-in-progress';
      case 'COMPLETADO': return 'status-completed';
      default: return '';
    }
  }

  // Obtener icono según estado
  getStatusIcon(status: string): string {
    switch (status) {
      case 'PENDIENTE': return 'schedule';
      case 'EN_PROCESO': return 'sync';
      case 'COMPLETADO': return 'check_circle';
      default: return 'help';
    }
  }

  // Métodos para contar estados de generadores
  getPendingCount(roadmap: EmployeeRoadmapResponse): number {
    return roadmap.generators.filter(gen => gen.status === 'PENDIENTE').length;
  }

  getInProgressCount(roadmap: EmployeeRoadmapResponse): number {
    return roadmap.generators.filter(gen => gen.status === 'EN_PROCESO').length;
  }

  getCompletedCount(roadmap: EmployeeRoadmapResponse): number {
    return roadmap.generators.filter(gen => gen.status === 'COMPLETADO').length;
  }

  // Refresh de datos
  refresh(): void {
    this.loadRoadmaps();
  }

  // 🆕 NUEVO: Ver ruta optimizada
  viewOptimizedRoute(roadmapId: number): void {
    this.router.navigate(['/empleado/route', roadmapId]);
  }
  // Logout
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    this.router.navigate(['/login']);
  }
  canCloseRoadmap(roadmap: EmployeeRoadmapResponse): boolean {
    // Se puede cerrar cualquier ruta que tenga precintos asignados
    // (no importa si están recolectados o no)
    return roadmap.generators.some(gen => gen.totalBags > 0);
  }
  getCloseButtonText(roadmap: EmployeeRoadmapResponse): string {
    const progress = this.getRoadmapProgress(roadmap);

    if (progress.completed === 0) {
      return 'Finalizar sin Recolección';
    } else if (progress.completed === progress.total) {
      return 'Finalizar Ruta Completa';
    } else {
      return 'Finalizar Ruta Parcial';
    }
  }

// 🆕 NUEVO MÉTODO: Obtener clase CSS del botón
  getCloseButtonClass(roadmap: EmployeeRoadmapResponse): string {
    const progress = this.getRoadmapProgress(roadmap);

    if (progress.completed === 0) {
      return 'warn'; // Botón amarillo/naranja para sin recolección
    } else if (progress.completed === progress.total) {
      return 'accent'; // Botón verde para completado
    } else {
      return 'primary'; // Botón azul para parcial
    }
  }

// 🆕 NUEVO MÉTODO: Abrir modal de cierre
  openCloseRoadmapModal(roadmap: EmployeeRoadmapResponse): void {
    console.log('🏁 Abriendo modal de cierre para ruta:', roadmap.roadmapId);

    const dialogRef = this.dialog.open(CloseRoadmapModalComponent, {
      width: '90vw',
      maxWidth: '600px',
      maxHeight: '90vh',
      data: { roadmap: roadmap },
      disableClose: false
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // El usuario confirmó el cierre
        this.processRoadmapClose(result as RoadmapCloseRequest);
      }
    });
  }

// 🆕 NUEVO MÉTODO: Procesar cierre de ruta
  private processRoadmapClose(request: RoadmapCloseRequest): void {
    console.log('🔄 Procesando cierre de ruta:', request);

    // Mostrar indicador de carga
    this.loading = true;

    this.empleadoService.closeRoadmap(request).subscribe({
      next: (response: RoadmapCloseResponse) => {
        console.log('✅ Ruta cerrada exitosamente:', response);

        // Mostrar mensaje de éxito
        this.showSuccessMessage(response);

        // Recargar las rutas para mostrar el nuevo estado
        this.loadRoadmaps();
      },
      error: (error) => {
        console.error('❌ Error cerrando ruta:', error);
        this.loading = false;

        // Mostrar mensaje de error
        this.showErrorMessage(error);
      }
    });
  }

// 🆕 NUEVO MÉTODO: Mostrar mensaje de éxito
  private showSuccessMessage(response: RoadmapCloseResponse): void {
    const message = `🏁 ${response.message} - ${response.sealsDelivered} precintos entregados en planta`;

    this.snackBar.open(message, 'Cerrar', {
      duration: 8000,
      panelClass: ['success-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

// 🆕 NUEVO MÉTODO: Mostrar mensaje de error
  private showErrorMessage(error: any): void {
    let errorMessage = 'Error al cerrar la hoja de ruta';

    if (error.error?.error) {
      errorMessage = error.error.error;
    } else if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    this.snackBar.open(`❌ ${errorMessage}`, 'Cerrar', {
      duration: 8000,
      panelClass: ['error-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }
}
