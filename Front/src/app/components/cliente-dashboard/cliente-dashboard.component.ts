import {Component, inject, TemplateRef, ViewChild, OnInit} from '@angular/core';
import {MatToolbar, MatToolbarModule} from "@angular/material/toolbar";
import {MatCard, MatCardModule, MatCardTitle} from "@angular/material/card";
import {Router, RouterLink} from "@angular/router";
import {MatDivider, MatDividerModule} from "@angular/material/divider";
import {MatButtonModule} from "@angular/material/button";
import {MatIconModule} from "@angular/material/icon";
import {ClienteServiceService} from "../../services/cliente-service.service";
import {MatDialog, MatDialogActions, MatDialogContent, MatDialogModule} from "@angular/material/dialog";
import {MatFormField, MatFormFieldModule, MatLabel} from "@angular/material/form-field";
import {
  MatDatepicker,
  MatDatepickerInput,
  MatDatepickerModule,
  MatDatepickerToggle
} from "@angular/material/datepicker";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {MatNativeDateModule} from "@angular/material/core";
import {MatInputModule} from "@angular/material/input";
import {RetiroService} from "../../services/Retiro/retiro.service";
import {MatList, MatListItem} from "@angular/material/list";
import {DatePipe, NgForOf, NgIf} from "@angular/common";
import {AdminService} from "../../services/Admin/admin.service";
import { CommonModule } from '@angular/common';
import {MatSelectModule} from "@angular/material/select";
import {HttpClient} from "@angular/common/http";
import {GeneratorService} from "../../services/Generador/generator.service";

// Interfaces para el nuevo endpoint
interface RequestOrderDto {
  zoneId: number;
  userId: string;
  scheduledDate: string; // ISO string
  countBags: number;
}

interface OrdersDto {
  id: number;
  state: string;
  generador: string;
  address: string;
  latitude?: number;
  longitude?: number;
  employeeId?: number;
  employeeName?: string;
  employeeState?: string;
  zone: string;
  creationDate: string;
  scheduledDate: string;
  completedDate?: string;
  priority?: number;
  priorityText?: string;
  notes?: string;
}

interface Zona {
  id: number;
  name: string;
}

@Component({
  selector: 'app-cliente-dashboard',
  standalone: true,
  imports: [
    MatToolbar,
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
    MatNativeDateModule,
    MatList,
    DatePipe,
    MatListItem,
    NgIf,
    NgForOf,
    MatSelectModule,
    CommonModule
  ],
  templateUrl: './cliente-dashboard.component.html',
  styleUrl: './cliente-dashboard.component.css'
})
export class ClienteDashboardComponent implements OnInit {
  nombreCliente = 'Franco';
  router = inject(Router);
  service = inject(ClienteServiceService);
  retiroService = inject(RetiroService);
  generatorService = inject(GeneratorService);
  adminService = inject(AdminService);
  http = inject(HttpClient);

  @ViewChild('retiroModal') retiroModalTemplate: any;

  retiroForm: FormGroup;
  zonas: Zona[] = [];
  currentUserId: string | null = ''; // Este valor debería venir del usuario logueado

  private readonly orderApiUrl = 'http://localhost:8081/Order';

  constructor(
    private dialog: MatDialog,
    private fb: FormBuilder,
  ) {
    this.retiroForm = this.fb.group({
      zoneId: ['', Validators.required],
      scheduledDate: ['', Validators.required],
      countBags: ['', [Validators.required, Validators.min(1)]],
    });
  }

  ngOnInit() {
    this.loadZonas();
    // Aquí deberías obtener el generatorId del usuario logueado
    this.currentUserId = localStorage.getItem('userId');

  }

  loadZonas() {
    console.log('Cargando zonas...');
    this.adminService.getZonas().subscribe({
      next: (zonas) => {
        console.log('Respuesta del servidor para zonas:', zonas);
        console.log('Tipo de respuesta:', typeof zonas);
        console.log('Es array:', Array.isArray(zonas));

        if (zonas && zonas.length > 0) {
          console.log('Primera zona:', zonas[0]);
          console.log('Estructura de primera zona:', Object.keys(zonas[0]));
        }

        this.zonas = zonas || [];
        console.log('Zonas asignadas al component:', this.zonas);
      },
      error: (error) => {
        console.error('Error detallado al cargar zonas:', error);
        alert(`Error al cargar las zonas disponibles: ${error.message || 'Error desconocido'}`);
      }
    });
  }

  irAVenta() {
    this.router.navigate(['/venta']);
  }

  confirmarRetiro(): void {
    console.log('=== DEBUG CONFIRMACIÓN RETIRO ===');
    console.log('Formulario válido:', this.retiroForm.valid);
    console.log('Valores del formulario:', this.retiroForm.value);
    console.log('Errores del formulario:', this.retiroForm.errors);
    console.log('Zonas disponibles:', this.zonas);

    if (this.retiroForm.valid) {
      const formValues = this.retiroForm.value;
      if (!this.currentUserId) {
        console.error('Usuario no encontrado');
        // Redirigir al login o mostrar error
        return;
      }
      // Crear el DTO según la nueva estructura
      const requestOrderDto: RequestOrderDto = {
        zoneId: Number(formValues.zoneId),
        userId: this.currentUserId,
        scheduledDate: formValues.scheduledDate.toISOString(),
        countBags: Number(formValues.countBags)
      };

      console.log('DTO a enviar:', requestOrderDto);

      // Llamar al nuevo endpoint
      this.http.post<OrdersDto>(`${this.orderApiUrl}/crear`, requestOrderDto).subscribe({
        next: (response) => {
          console.log('Orden creada exitosamente:', response);
          alert(`Solicitud de retiro creada exitosamente. ID de orden: ${response.id}`);
          this.retiroForm.reset();
          this.dialog.closeAll();
        },
        error: (error) => {
          console.error('Error al crear la orden:', error);
          let errorMessage = 'Error al crear la solicitud de retiro';

          if (error.error && typeof error.error === 'string') {
            errorMessage = error.error;
          } else if (error.message) {
            errorMessage = error.message;
          }

          alert(errorMessage);
        }
      });
    } else {
      // Mostrar errores de validación
      console.log('=== ERRORES DE VALIDACIÓN ===');
      Object.keys(this.retiroForm.controls).forEach(key => {
        const control = this.retiroForm.get(key);
        if (control?.errors) {
          console.log(`${key}:`, control.errors);
        }
      });

      this.markFormGroupTouched(this.retiroForm);
      alert('Por favor, complete todos los campos requeridos');
    }
  }

  private markFormGroupTouched(formGroup: FormGroup) {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  openModal() {
    // Debug: verificar zonas antes de abrir el modal
    console.log('Zonas disponibles al abrir modal:', this.zonas);

    if (this.zonas.length === 0) {
      console.log('No hay zonas cargadas, recargando...');
      this.loadZonas();
    }

    this.dialog.open(this.retiroModalTemplate, {
      width: '600px',
      disableClose: false
    });
  }

  cerrarSesion() {
    this.service.logout();
    this.router.navigate(['/login']);
  }

  protected readonly opener = opener;



  // Métodos de validación para el template
  getErrorMessage(fieldName: string): string {
    const control = this.retiroForm.get(fieldName);

    if (control?.hasError('required')) {
      switch(fieldName) {
        case 'zoneId':
          return 'Debe seleccionar una zona';
        case 'scheduledDate':
          return 'Debe seleccionar fecha y hora';
        case 'countBags':
          return 'Debe indicar la cantidad de bolsas';
        default:
          return `${fieldName} es requerido`;
      }
    }

    if (control?.hasError('min')) {
      return 'La cantidad debe ser mayor a 0';
    }

    return '';
  }

  hasError(fieldName: string): boolean {
    const control = this.retiroForm.get(fieldName);
    return !!(control?.invalid && (control?.dirty || control?.touched));
  }

  // Filtro para días hábiles (lunes a viernes)
  dateFilter = (date: Date | null): boolean => {
    if (!date) {
      return false;
    }
    const day = date.getDay();
    // 0 = domingo, 6 = sábado - solo permitir lunes (1) a viernes (5)
    return day !== 0 && day !== 6;
  };

  // Fecha mínima (mañana)
  get minDate(): Date {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow;
  }

  // Fecha máxima (3 meses desde hoy)
  get maxDate(): Date {
    const maxDate = new Date();
    maxDate.setMonth(maxDate.getMonth() + 3);
    return maxDate;
  }

  // Track by function para el ngFor
  trackByZoneId(index: number, zona: Zona): number {
    return zona.id;
  }

  // Event handler para debug del selector
  onZoneChange(event: any) {
    console.log('Zona seleccionada:', event.value);
    console.log('Zona completa:', this.zonas.find(z => z.id === event.value));
  }
}
