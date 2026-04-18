import {Component, inject, TemplateRef, ViewChild} from '@angular/core';
import {MatTable, MatTableModule} from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import {MatList, MatListItem, MatListModule, MatListOption, MatNavList, MatSelectionList} from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import {MatToolbar, MatToolbarModule} from "@angular/material/toolbar";
import {MatCard, MatCardModule, MatCardTitle} from "@angular/material/card";
import {MatDivider, MatDividerModule} from "@angular/material/divider";
import {MatDialog, MatDialogActions, MatDialogContent, MatDialogModule} from "@angular/material/dialog";
import {MatFormField, MatFormFieldModule, MatLabel} from "@angular/material/form-field";
import {
  MatDatepicker,
  MatDatepickerInput,
  MatDatepickerModule,
  MatDatepickerToggle
} from "@angular/material/datepicker";
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule, ValidationErrors,
  Validators
} from "@angular/forms";
import {MatInputModule} from "@angular/material/input";
import {MatNativeDateModule, MatOption} from "@angular/material/core";
import {MatSidenav, MatSidenavContainer, MatSidenavContent} from "@angular/material/sidenav";
import {CommonModule} from "@angular/common";
import {MatSelect} from "@angular/material/select";
import {GeneratorService} from "../../services/Generador/generator.service";
import {Generador} from "../../models/Generador";
import {Vehiculo} from "../../models/Vehiculo";
import {Empleado} from "../../models/Empleado";
import {Zona} from "../../models/Zona";
import {Pedido} from "../../models/Pedido";
import {MatSnackBar} from "@angular/material/snack-bar";
import { ChangeDetectorRef } from '@angular/core';
import {ViewEncapsulation } from '@angular/core';
import {CreateRoadmapRequest, StockInfo} from "../../models/Interfaces";
import {AdminService} from "../../services/Admin/admin.service";
import { Router } from '@angular/router'; // 🆕 Importar Router

@Component({
  selector: 'app-admin',
  standalone: true,
  encapsulation: ViewEncapsulation.None,

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
    MatNativeDateModule, MatTable, MatListItem, MatList, MatNavList, MatSidenavContainer, MatSidenav, MatSidenavContent, CommonModule, MatSelect, MatOption, MatSelectionList, FormsModule, MatListOption],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent {
  @ViewChild('dialogTemplate') dialogTemplate!: TemplateRef<any>;
  @ViewChild('dialogVehiculo') dialogVehiculo!: TemplateRef<any>;
  @ViewChild('dialogEmpleado') dialogEmpleado!: TemplateRef<any>;

  rutaForm: FormGroup;
  vehiculoForm: FormGroup;
  empleadoForm: FormGroup;

  zonaSeleccionada: string = '';
  selectedPedidos: Pedido[] = [];
  selectedPedidoIds: number[] = [];

  pedidos: Pedido[] = [];
  vehiculos: Vehiculo[] = [];
  tipos: String[] = ['RECOLECTOR', 'TRATADOR']
  zonas: Zona[] = [];
  operarios: Empleado[] = [];
  solicitudes: Generador[] = [];
  generadores: Generador[] = [];

  stockInfo: StockInfo[] = [];
  lowStock: StockInfo[] = [];

  minDate = new Date();
  generatorService = inject(GeneratorService);
  adminService = inject(AdminService);

  constructor(
    protected dialog: MatDialog,
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    private router: Router // 🆕 Inyectar Router

  ) {
    this.rutaForm = this.fb.group({
      vehiculo: ['', Validators.required],
      zona: ['', Validators.required],
      operario: ['', Validators.required],
      horaSalida: ['', Validators.required],
      fechaRecoleccion: ['', [Validators.required, this.fechaValidaValidator.bind(this)]],
      pedidos: [[], Validators.required],
    });

    this.vehiculoForm = this.fb.group({
      patent: ['', Validators.required],
      type: ['', Validators.required],
    });

    this.empleadoForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      type: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargarDatosIniciales();
    this.cargarGeneradoresActivos();
    this.cargarGeneradoresPendientes();
    this.cargarStock();
  }

  // 🆕 MÉTODOS PARA NAVEGACIÓN A REPORTES
  irAReporteZona(): void {
    // Prevenir comportamiento de link
    this.router.navigate(['/reporte-zona']).then(() => {
      // Forzar re-render de botones para mantener estilos
      this.cdr.detectChanges();
    });
  }

  irAReporteVenta(): void {
    this.router.navigate(['/reporte-venta']).then(() => {
      this.cdr.detectChanges();
    });
  }

  irAReporteTratamiento(): void {
    this.router.navigate(['/reporte-tratamiento']).then(() => {
      this.cdr.detectChanges();
    });
  }
  ngAfterViewInit(): void {
    // Forzar estilos de botones después de que la vista se inicializa
    setTimeout(() => {
      this.forceButtonStyles();
    }, 100);
  }

// 🆕 Método helper para forzar estilos
  private forceButtonStyles(): void {
    const buttons = document.querySelectorAll('.nav-button');
    buttons.forEach((button: Element) => {
      const htmlButton = button as HTMLElement;
      // Remover cualquier estilo inline problemático
      htmlButton.style.removeProperty('color');
      htmlButton.style.removeProperty('background-color');

      // Re-aplicar clases para forzar estilos CSS
      if (htmlButton.classList.contains('report-action')) {
        htmlButton.style.setProperty('background', '#00B5B8', 'important');
        htmlButton.style.setProperty('color', 'white', 'important');
      } else if (htmlButton.classList.contains('primary-action')) {
        htmlButton.style.setProperty('background', '#00A650', 'important');
        htmlButton.style.setProperty('color', 'white', 'important');
      } else if (htmlButton.classList.contains('secondary-action')) {
        htmlButton.style.setProperty('background', '#0066CC', 'important');
        htmlButton.style.setProperty('color', 'white', 'important');
      }
    });
  }

  cargarDatosIniciales(): void {
    this.adminService.getZonas().subscribe({
      next: (data) => this.zonas = data,
      error: (err) => console.error('Error al cargar zonas:', err),
    });

    this.adminService.getEmpleados().subscribe({
      next: (data) => this.operarios = data,
      error: (err) => console.error('Error al cargar empleados:', err),
    });

    this.adminService.getVehiculos().subscribe({
      next: (data) => this.vehiculos = data,
      error: (err) => console.error('Error al cargar vehículos:', err),
    });
  }

  onZonaSeleccionada(zona: string): void {
    this.zonaSeleccionada = zona;
    this.rutaForm.get('zona')?.setValue(zona);

    this.adminService.getPedidosPorZona(zona).subscribe({
      next: (data) => {
        this.pedidos = data;
        this.selectedPedidos = [];
        this.selectedPedidoIds = [];
        // Limpiar la selección del formulario
        this.rutaForm.get('pedidos')?.setValue([]);
        console.log(data);
      },
      error: (err) => console.error('Error al obtener pedidos:', err),
    });
  }

  fechaValidaValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    const fechaSeleccionada = new Date(control.value);
    const hoy = new Date();

    hoy.setHours(0, 0, 0, 0);
    fechaSeleccionada.setHours(0, 0, 0, 0);

    if (fechaSeleccionada < hoy) {
      return { fechaAnterior: { value: control.value } };
    }

    const diaSemana = fechaSeleccionada.getDay(); // 0=domingo, 1=lunes, ..., 6=sábado
    if (diaSemana === 0 || diaSemana === 6) { // domingo o sábado
      return { noEsDiaHabil: { value: control.value } };
    }

    return null; // Fecha válida
  }

  dateFilter = (date: Date | null): boolean => {
    if (!date) {
      return false;
    }

    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    date.setHours(0, 0, 0, 0);

    if (date < hoy) {
      return false;
    }

    const diaSemana = date.getDay();
    return diaSemana !== 0 && diaSemana !== 6;
  };

  onPedidosSelectionChange(event: any): void {
    const selectedOptions = event.source.selectedOptions.selected;
    this.selectedPedidos = selectedOptions.map((option: any) => option.value);
    this.selectedPedidoIds = this.selectedPedidos.map(pedido => pedido.id);
    console.log('Pedidos seleccionados:', this.selectedPedidoIds);
  }

  crearRuta(): void {
    if (this.rutaForm.invalid) {
      this.mostrarError('Por favor complete todos los campos requeridos');
      return;
    }

    const pedidosSeleccionados = this.rutaForm.get('pedidos')?.value || [];
    if (pedidosSeleccionados.length === 0) {
      this.mostrarError('Debe seleccionar al menos un pedido');
      return;
    }

    const formValues = this.rutaForm.value;

    const fechaRecoleccion = new Date(formValues.fechaRecoleccion);
    const horaSalida = formValues.horaSalida;

    const fechaHoraCompleta = new Date(fechaRecoleccion);
    const [horas, minutos] = horaSalida.split(':');
    fechaHoraCompleta.setHours(parseInt(horas), parseInt(minutos), 0, 0);

    const selectedOrderIds = pedidosSeleccionados.map((pedido: any) => {
      if (typeof pedido === 'object' && pedido.id) {
        return Number(pedido.id);
      }
      return Number(pedido);
    });

    // ✅ DEBUG: Ver qué estamos enviando
    console.log('Pedidos seleccionados (objetos):', pedidosSeleccionados);
    console.log('IDs extraídos (números):', selectedOrderIds);

    // ✅ CORRECCIÓN: Manejar empleado y vehículo también
    let employeeName = formValues.operario;
    if (typeof employeeName === 'object' && employeeName.name) {
      employeeName = employeeName.name;
    }

    let vehicleId = formValues.vehiculo;
    if (typeof vehicleId === 'object' && vehicleId.id) {
      vehicleId = vehicleId.id;
    }

    const createRoadmapRequest: CreateRoadmapRequest = {
      zone: this.zonaSeleccionada,
      employee: employeeName,
      collectDate: fechaHoraCompleta.toISOString(),
      exitHour: horaSalida + ':00',
      selectedOrderIds: selectedOrderIds, // ✅ Solo números
      vehicleId: vehicleId ? Number(vehicleId) : undefined
    };

    console.log('Request final a enviar:', createRoadmapRequest);

    this.adminService.createRoadmap(createRoadmapRequest).subscribe({
      next: (response: any) => {
        console.log('Hoja de ruta creada:', response);

        const message = response.message || 'Hoja de ruta creada exitosamente';
        const roadmapId = response.roadmapId || 'N/A';

        this.mostrarExito(`${message} (ID: ${roadmapId})`);

        this.dialog.closeAll();
        this.resetearFormulario();

        if (this.zonaSeleccionada) {
          this.onZonaSeleccionada(this.zonaSeleccionada);
        }
      },
      error: (err) => {
        console.error('Error completo:', err);
        console.error('Error body:', err.error);

        let errorMessage = 'Error al crear la hoja de ruta';

        if (err.error && err.error.error) {
          errorMessage = err.error.error;
        } else if (err.status === 400) {
          errorMessage = 'Datos inválidos. Verifique la información ingresada.';
        } else if (err.status === 500) {
          errorMessage = 'Error interno del servidor. Intente nuevamente.';
        } else if (err.status === 0) {
          errorMessage = 'No se pudo conectar con el servidor.';
        }

        this.mostrarError(errorMessage);
      }
    });
  }

  resetearFormulario(): void {
    this.rutaForm.reset();
    this.selectedPedidos = [];
    this.selectedPedidoIds = [];
    this.zonaSeleccionada = '';
    this.pedidos = [];

    // Forzar detección de cambios
    this.cdr.detectChanges();
  }

  mostrarExito(mensaje: string): void {
    this.snackBar.open(mensaje, 'Cerrar', {
      duration: 5000,
      panelClass: ['success-snackbar']
    });
  }

  mostrarError(mensaje: string): void {
    this.snackBar.open(mensaje, 'Cerrar', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }

  abrirModal(): void {
    if (this.dialog.openDialogs.length === 0) {
      this.dialog.open(this.dialogTemplate, {
        width: '600px',
        maxHeight: '80vh'
      });
    }
  }

  abrirModalVehiculo(): void {
    this.dialog.open(this.dialogVehiculo);
  }

  abrirModalEmpleado(): void {
    this.dialog.open(this.dialogEmpleado);
  }

  registrarVehiculo(): void {
    if (this.vehiculoForm.valid) {
      this.adminService.registrarVehiculo(this.vehiculoForm.value).subscribe({
        next: () => {
          this.dialog.closeAll();
          this.cargarDatosIniciales();
          this.mostrarExito('Vehículo registrado exitosamente');
        },
        error: (err) => {
          console.error(err);
          this.mostrarError('Error al registrar vehículo');
        }
      });
    }
  }

  registrarEmpleado(): void {
    if (this.empleadoForm.valid) {
      this.adminService.registrarEmpleado(this.empleadoForm.value).subscribe({
        next: () => {
          this.dialog.closeAll();
          this.cargarDatosIniciales();
          this.mostrarExito('Empleado registrado exitosamente');
        },
        error: (err) => {
          console.error(err);
          this.mostrarError('Error al registrar empleado');
        }
      });
    }
  }

  cargarGeneradoresActivos(): void {
    this.generatorService.getGeneradoresActivos().subscribe({
      next: (data) => this.generadores = data,
      error: (err) => console.error('Error al cargar generadores activos:', err),
    });
  }

  cargarGeneradoresPendientes(): void {
    this.generatorService.getGeneradoresPendientes().subscribe({
      next: (data) => this.solicitudes = data,
      error: (err) => console.error('Error al cargar generadores pendientes:', err),
    });
  }

  aprobarSolicitud(solicitud: number | undefined): void {
    console.log('Solicitud aprobada:', solicitud);
    this.adminService.aprobarSolicitud(solicitud).subscribe({
      next: () => {
        this.cargarDatosIniciales();
        this.cargarGeneradoresActivos();
        this.cargarGeneradoresPendientes();
        this.mostrarExito('Solicitud aprobada exitosamente');
      },
      error: (err) => {
        console.error('Error al aprobar solicitud:', err);
        this.mostrarError('No se pudo aprobar la solicitud');
      }
    });
  }

  // ===== NUEVOS MÉTODOS PARA STOCK =====

  cargarStock(): void {
    this.adminService.getStock().subscribe({
      next: (data) => {
        this.stockInfo = data;
        this.lowStock = data.filter(stock => stock.lowStock);
        console.log('Stock cargado:', this.stockInfo);
        console.log('Stock bajo:', this.lowStock);
      },
      error: (err) => {
        console.error('Error al cargar stock:', err);
        this.mostrarError('Error al cargar información de stock');
      }
    });
  }

  solicitarReposicion(bagId: number, size: string): void {
    this.adminService.solicitarReposicion(bagId).subscribe({
      next: (response) => {
        this.mostrarExito(`Solicitud de reposición enviada para bolsas ${size}`);
        this.cargarStock(); // Recargar stock
      },
      error: (err) => {
        console.error('Error al solicitar reposición:', err);
        this.mostrarError('Error al enviar solicitud de reposición');
      }
    });
  }

  incrementarStock(bagId: number, quantity: number, size: string): void {
    this.adminService.incrementarStock(bagId, quantity).subscribe({
      next: (response) => {
        this.mostrarExito(`Stock incrementado: ${quantity} bolsas ${size}`);
        this.cargarStock(); // Recargar stock
      },
      error: (err) => {
        console.error('Error al incrementar stock:', err);
        this.mostrarError('Error al incrementar stock');
      }
    });
  }
  getLowStockCount(): number {
    return this.stockInfo.filter(stock => stock.lowStock).length;
  }
}
