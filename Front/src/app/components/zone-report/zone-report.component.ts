import {Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy, inject} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';
import * as XLSX from 'xlsx';
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatCardModule } from "@angular/material/card";
import { MatSelectModule } from "@angular/material/select";
import { MatOptionModule } from "@angular/material/core";
import { DatePipe, NgForOf, NgIf } from "@angular/common";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
import { MatProgressBar } from "@angular/material/progress-bar";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import {ZoneReportDto, ZoneReportFiltersDto, ZoneReportService} from "../../services/zone-report/zone-report.service";

// Interfaces
export interface ZoneReportData {
  zona: string;
  totalSolicitudes: number;
  solicitudesPendientes: number;
  solicitudesCompletadas: number;
  totalBolsas: number;
  fechaUltimaSolicitud: string;
  promedioDiasProcesamiento: number;
  porcentajeCompletado: number;
}

export interface ReportKPIs {
  totalSolicitudesGlobal: number;
  zonaConMasSolicitudes: string;
  promedioSolicitudesPorZona: number;
  porcentajeCompletadoGeneral: number;
  totalBolsasRecolectadas: number;
  tiempoPromedioRespuesta: number;
}

@Component({
  selector: 'app-zone-report',
  templateUrl: './zone-report.component.html',
  standalone: true,
  imports: [
    // Angular Common
    NgIf,
    NgForOf,
    DatePipe,
    ReactiveFormsModule,

    // Material Modules
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatOptionModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatProgressSpinner,
    MatProgressBar
  ],
  styleUrls: ['./zone-report.component.css']
})
export class ZoneReportComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild('barChart') barChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('pieChart') pieChartRef!: ElementRef<HTMLCanvasElement>;

  // Servicios
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly zoneReportService = inject(ZoneReportService);

  // Formularios y datos
  filterForm: FormGroup;
  reportData: ZoneReportData[] = [];
  availableZones: string[] = [];
  kpis: ReportKPIs = {
    totalSolicitudesGlobal: 0,
    zonaConMasSolicitudes: '',
    promedioSolicitudesPorZona: 0,
    porcentajeCompletadoGeneral: 0,
    totalBolsasRecolectadas: 0,
    tiempoPromedioRespuesta: 0
  };

  // Tabla
  dataSource = new MatTableDataSource<ZoneReportData>();
  displayedColumns: string[] = [
    'zona',
    'totalSolicitudes',
    'solicitudesPendientes',
    'solicitudesCompletadas',
    'totalBolsas',
    'porcentajeCompletado',
    'fechaUltimaSolicitud',
    'promedioDiasProcesamiento'
  ];

  // Charts
  barChart: Chart | null = null;
  pieChart: Chart | null = null;

  // Estados
  isLoading = false;
  filtrosAplicados = false;

  constructor() {
    // Registrar componentes de Chart.js
    Chart.register(...registerables);

    this.filterForm = this.fb.group({
      fechaInicio: [''],
      fechaFin: [''],
      estado: ['TODOS'],
      zona: ['TODAS']
    });

    // Establecer fechas por defecto (últimos 30 días)
    const today = new Date();
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(today.getDate() - 30);

    this.filterForm.patchValue({
      fechaInicio: thirtyDaysAgo.toISOString().split('T')[0],
      fechaFin: today.toISOString().split('T')[0]
    });
  }

  ngOnInit(): void {
    this.snackBar.dismiss();
    this.loadAvailableZones();
    console.log('ZoneReportComponent inicializado');
  }

  ngAfterViewInit(): void {
    if (this.sort) {
      this.dataSource.sort = this.sort;
    }
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;
    }
  }

  /**
   * Carga las zonas disponibles desde el backend
   */
  loadAvailableZones(): void {
    this.zoneReportService.getAvailableZones().subscribe({
      next: (zones) => {
        this.availableZones = zones;
        console.log('Zonas cargadas:', zones);
      },
      error: (error) => {
        console.error('Error al cargar zonas:', error);
        this.showError('Error al cargar las zonas disponibles');
        // Usar zonas por defecto en caso de error
        this.availableZones = ['Zona Centro', 'Zona Norte', 'Zona Sur', 'Zona Este', 'Zona Oeste'];
      }
    });
  }

  /**
   * Genera el reporte usando el servicio real
   */
  generateReport(): void {
    console.log('Generando reporte...');
    this.snackBar.dismiss();
    this.isLoading = true;

    const filters: ZoneReportFiltersDto = this.buildFilters();

    this.zoneReportService.generateZoneReport(filters).subscribe({
      next: (response) => {
        if (response.success) {
          this.reportData = this.mapToLocalInterface(response.data);
          this.kpis = response.kpis;
          this.dataSource.data = this.reportData;
          this.updateCharts();
          this.filtrosAplicados = true;
          this.showSuccess('Reporte generado exitosamente');
        } else {
          this.showError(response.message || 'Error al generar el reporte');
        }
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        this.showError('Error al generar el reporte: ' + (error.message || 'Error desconocido'));
        console.error('Error:', error);
      }
    });
  }

  /**
   * Construye los filtros para enviar al backend
   */
  private buildFilters(): ZoneReportFiltersDto {
    const formValue = this.filterForm.value;
    return {
      fechaInicio: formValue.fechaInicio || undefined,
      fechaFin: formValue.fechaFin || undefined,
      estado: formValue.estado === 'TODOS' ? undefined : formValue.estado,
      zona: formValue.zona === 'TODAS' ? undefined : formValue.zona
    };
  }

  /**
   * Mapea la respuesta del backend a la interfaz local
   */
  private mapToLocalInterface(data: ZoneReportDto[]): ZoneReportData[] {
    return data.map(item => ({
      zona: item.zona,
      totalSolicitudes: item.totalSolicitudes,
      solicitudesPendientes: item.solicitudesPendientes,
      solicitudesCompletadas: item.solicitudesCompletadas,
      totalBolsas: item.totalBolsas,
      fechaUltimaSolicitud: item.fechaUltimaSolicitud,
      promedioDiasProcesamiento: item.promedioDiasProcesamiento,
      porcentajeCompletado: item.porcentajeCompletado
    }));
  }

  updateCharts(): void {
    setTimeout(() => {
      this.createBarChart();
      this.createPieChart();
    }, 100);
  }

  createBarChart(): void {
    if (this.barChart) {
      this.barChart.destroy();
    }

    const ctx = this.barChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    const data = this.reportData.sort((a, b) => b.totalSolicitudes - a.totalSolicitudes);

    this.barChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: data.map(item => item.zona),
        datasets: [
          {
            label: 'Solicitudes Totales',
            data: data.map(item => item.totalSolicitudes),
            backgroundColor: '#0066CC',
            borderColor: '#004A99',
            borderWidth: 1
          },
          {
            label: 'Completadas',
            data: data.map(item => item.solicitudesCompletadas),
            backgroundColor: '#00A650',
            borderColor: '#007A3D',
            borderWidth: 1
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Solicitudes por Zona'
          },
          legend: {
            position: 'top'
          }
        },
        scales: {
          y: {
            beginAtZero: true
          }
        }
      }
    });
  }

  createPieChart(): void {
    if (this.pieChart) {
      this.pieChart.destroy();
    }

    const ctx = this.pieChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    const data = this.reportData.sort((a, b) => b.totalSolicitudes - a.totalSolicitudes);

    this.pieChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: data.map(item => item.zona),
        datasets: [{
          data: data.map(item => item.totalSolicitudes),
          backgroundColor: [
            '#0066CC',
            '#00A650',
            '#FF6600',
            '#00B5B8',
            '#6B46C1'
          ],
          borderWidth: 2,
          borderColor: '#ffffff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Distribución de Solicitudes'
          },
          legend: {
            position: 'right'
          }
        }
      }
    });
  }

  clearFilters(): void {
    console.log('Limpiando filtros...');
    this.snackBar.dismiss();

    // Establecer fechas por defecto nuevamente
    const today = new Date();
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(today.getDate() - 30);

    this.filterForm.reset({
      fechaInicio: thirtyDaysAgo.toISOString().split('T')[0],
      fechaFin: today.toISOString().split('T')[0],
      estado: 'TODOS',
      zona: 'TODAS'
    });
    this.filtrosAplicados = false;
  }

  /**
   * Exporta el reporte a Excel usando el servicio
   */
  exportToExcel(): void {
    const filters = this.buildFilters();

    this.zoneReportService.exportToExcel(filters).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `reporte_zonas_${new Date().toISOString().split('T')[0]}.xlsx`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        this.showSuccess('Reporte exportado exitosamente');
      },
      error: (error) => {
        this.showError('Error al exportar el reporte a Excel');
        console.error('Error:', error);
      }
    });
  }

  /**
   * Exporta el reporte a PDF usando el servicio
   */
  exportToPDF(): void {
    const filters = this.buildFilters();

    this.zoneReportService.exportToPDF(filters).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `reporte_zonas_${new Date().toISOString().split('T')[0]}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        this.showSuccess('Reporte exportado exitosamente');
      },
      error: (error) => {
        this.showError('Error al exportar el reporte a PDF');
        console.error('Error:', error);
      }
    });
  }

  getMaxSolicitudes(): number {
    if (this.reportData.length === 0) return 0;
    return Math.max(...this.reportData.map(item => item.totalSolicitudes));
  }

  goBack(): void {
    console.log('Navegando de vuelta al admin...');
    this.router.navigate(['/admin']);
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Cerrar', {
      duration: 3000,
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Cerrar', {
      duration: 3000,
      panelClass: ['error-snackbar']
    });
  }

  ngOnDestroy(): void {
    this.snackBar.dismiss();

    if (this.barChart) {
      this.barChart.destroy();
    }
    if (this.pieChart) {
      this.pieChart.destroy();
    }
  }
}
