import {Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy, inject} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatCardModule } from "@angular/material/card";
import { MatSelectModule } from "@angular/material/select";
import { MatOptionModule } from "@angular/material/core";
import {DatePipe, DecimalPipe, NgForOf, NgIf} from "@angular/common";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
import { MatProgressBar } from "@angular/material/progress-bar";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import {TreatmentReportService} from "../../services/tratamiento-reporte/treatment-report.service";


// Interfaces simplificadas sin operador
interface TreatmentReportFilters {
  fechaInicio?: string;
  fechaFin?: string;
  tipoGenerador?: string;
  zona?: string; // Cambiado de operador a zona
}

interface TreatmentReportData {
  fechaTratamiento: string;
  generador: string;
  tipoGenerador: string;
  zona: string; // Cambiado de operador a zona
  cantidadBolsas: number;
  pesoTotal: number;
}

interface TreatmentReportKPIs {
  totalTratamientos: number;
  totalBolsas: number;
  pesoTotal: number;
  promedioPesoPorBolsa: number;
  generadorTop: string; // Cambiado de operadorTop a generadorTop
  diaMasTratamientos: string;
}

interface TreatmentReportResponse {
  success: boolean;
  message: string;
  data: TreatmentReportData[];
  kpis: TreatmentReportKPIs;
}

@Component({
  selector: 'app-treatment-report',
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
    MatProgressBar,
    DecimalPipe
  ],
  templateUrl: './treatment-report.component.html',
  styleUrls: ['./treatment-report.component.css']
})
export class TreatmentReportComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild('barChart') barChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('pieChart') pieChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('topChart') topChartRef!: ElementRef<HTMLCanvasElement>;

  // Servicios
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly treatmentReportService = inject(TreatmentReportService);

  // Formularios y datos
  filterForm: FormGroup;
  reportData: TreatmentReportData[] = [];
  availableZones: string[] = []; // Cambiado de operadores a zonas
  kpis: TreatmentReportKPIs = {
    totalTratamientos: 0,
    totalBolsas: 0,
    pesoTotal: 0,
    promedioPesoPorBolsa: 0,
    generadorTop: '', // Cambiado de operadorTop
    diaMasTratamientos: ''
  };

  // Opciones para filtros (mismo estilo que zone-report)
  tiposGenerador = [
    { value: 'TODOS', label: 'Todos los Tipos' },
    { value: 'Público', label: 'Público' },
    { value: 'Privado', label: 'Privado' }
  ];

  // Tabla (mismo estilo que zone-report) - columnas sin operador
  dataSource = new MatTableDataSource<TreatmentReportData>();
  displayedColumns: string[] = [
    'fechaTratamiento',
    'generador',
    'tipoGenerador',
    'zona', // Mantenido zona en lugar de operador
    'cantidadBolsas',
    'pesoTotal'
  ];

  // Charts
  barChart: Chart | null = null;
  pieChart: Chart | null = null;
  topChart: Chart | null = null;

  // Estados (mismo que zone-report)
  isLoading = false;
  filtrosAplicados = false;

  constructor() {
    // Registrar componentes de Chart.js
    Chart.register(...registerables);

    this.filterForm = this.fb.group({
      fechaInicio: [''],
      fechaFin: [''],
      tipoGenerador: ['TODOS'],
      zona: ['TODAS'] // Cambiado de operador a zona
    });

    // Establecer fechas por defecto (últimos 30 días) - mismo que zone-report
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
    this.loadAvailableZones(); // Cambiado de operadores a zonas
    console.log('TreatmentReportComponent inicializado');
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
    this.treatmentReportService.getAvailableZones().subscribe({
      next: (zones) => {
        this.availableZones = zones;
        console.log('Zonas cargadas:', zones);
      },
      error: (error) => {
        console.error('Error al cargar zonas:', error);
        this.showError('Error al cargar zonas disponibles');
        // Usar zonas por defecto en caso de error
        this.availableZones = ['Centro', 'Nueva Córdoba', 'Barrio Güemes', 'Cerro de las Rosas'];
      }
    });
  }

  /**
   * Genera el reporte usando el servicio real (mismo patrón que zone-report)
   */
  generateReport(): void {
    console.log('Generando reporte de tratamientos...');
    this.snackBar.dismiss();
    this.isLoading = true;

    const filters = this.buildFilters();

    this.treatmentReportService.generateTreatmentReport(filters).subscribe({
      next: (response: TreatmentReportResponse) => {
        if (response.success) {
          this.reportData = response.data;
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
   * Construye los filtros para enviar al backend (mismo patrón que zone-report)
   */
  private buildFilters(): TreatmentReportFilters {
    const formValue = this.filterForm.value;
    return {
      fechaInicio: formValue.fechaInicio || undefined,
      fechaFin: formValue.fechaFin || undefined,
      tipoGenerador: formValue.tipoGenerador === 'TODOS' ? undefined : formValue.tipoGenerador,
      zona: formValue.zona === 'TODAS' ? undefined : formValue.zona
    };
  }

  updateCharts(): void {
    setTimeout(() => {
      this.createBarChart();
      this.createPieChart();
      this.createTopGeneratorsChart();
    }, 100);
  }

  createBarChart(): void {
    if (this.barChart) {
      this.barChart.destroy();
    }

    const ctx = this.barChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    // Agrupar por día
    const dailyData = this.groupByDate();
    const labels = Object.keys(dailyData).sort();
    const data = labels.map(date => dailyData[date].bolsas);
    const weights = labels.map(date => dailyData[date].peso);

    this.barChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Bolsas Tratadas',
            data: data,
            backgroundColor: '#0066CC',
            borderColor: '#004A99',
            borderWidth: 1
          },
          {
            label: 'Peso Total (kg)',
            data: weights,
            backgroundColor: '#00A650',
            borderColor: '#007A3D',
            borderWidth: 1,
            type: 'line',
            yAxisID: 'y1'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Tratamientos por Día'
          },
          legend: {
            position: 'top'
          }
        },
        scales: {
          y: {
            type: 'linear',
            display: true,
            position: 'left',
            beginAtZero: true
          },
          y1: {
            type: 'linear',
            display: true,
            position: 'right',
            beginAtZero: true,
            grid: {
              drawOnChartArea: false,
            },
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

    // Agrupar por tipo de generador (igual que ventas)
    const typeData = this.groupByType();
    const labels = Object.keys(typeData);
    const data = labels.map(type => typeData[type]);

    this.pieChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: [
            '#0066CC', // Azul para Público
            '#00A650'  // Verde para Privado
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
            text: 'Distribución por Tipo de Generador'
          },
          legend: {
            position: 'bottom'
          }
        }
      }
    });
  }

  createTopGeneratorsChart(): void {
    if (this.topChart) {
      this.topChart.destroy();
    }

    const ctx = this.topChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    // Agrupar por generador y tomar top 10
    const generatorData = this.groupByGenerator();
    const sortedGenerators = Object.entries(generatorData)
        .sort(([,a], [,b]) => b - a)
        .slice(0, 10);

    const labels = sortedGenerators.map(([name]) => name);
    const data = sortedGenerators.map(([,count]) => count);

    // Colores variados para cada barra
    const colors = [
      '#0066CC', '#00A650', '#FF6600', '#00B5B8', '#6B46C1',
      '#DC3545', '#FFC107', '#6F42C1', '#20C997', '#FD7E14'
    ];

    this.topChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: colors.slice(0, data.length),
          borderWidth: 1,
          borderColor: '#ffffff'
        }]
      },
      options: {
        indexAxis: 'y', // Barras horizontales
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Top 10 Generadores por Cantidad'
          },
          legend: {
            display: false
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            title: {
              display: true,
              text: 'Cantidad de Bolsas'
            }
          }
        }
      }
    });
  }

  clearFilters(): void {
    console.log('Limpiando filtros...');
    this.snackBar.dismiss();

    // Establecer fechas por defecto nuevamente (mismo que zone-report)
    const today = new Date();
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(today.getDate() - 30);

    this.filterForm.reset({
      fechaInicio: thirtyDaysAgo.toISOString().split('T')[0],
      fechaFin: today.toISOString().split('T')[0],
      tipoGenerador: 'TODOS',
      zona: 'TODAS'
    });
    this.filtrosAplicados = false;
  }

  /**
   * Exporta el reporte a Excel usando el servicio (mismo que zone-report)
   */
  exportToExcel(): void {
    const filters = this.buildFilters();

    this.treatmentReportService.exportToExcel(filters).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `reporte_tratamientos_${new Date().toISOString().split('T')[0]}.xlsx`;
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
   * Exporta el reporte a PDF usando el servicio (mismo que zone-report)
   */
  exportToPDF(): void {
    const filters = this.buildFilters();

    this.treatmentReportService.exportToPDF(filters).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `reporte_tratamientos_${new Date().toISOString().split('T')[0]}.pdf`;
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

  goBack(): void {
    console.log('Navegando de vuelta al admin...');
    this.router.navigate(['/admin']);
  }

  // Métodos de utilidad
  private groupByDate(): { [date: string]: { bolsas: number, peso: number } } {
    const grouped: { [date: string]: { bolsas: number, peso: number } } = {};

    this.reportData.forEach(item => {
      const date = new Date(item.fechaTratamiento).toLocaleDateString('es-AR');
      if (!grouped[date]) {
        grouped[date] = { bolsas: 0, peso: 0 };
      }
      grouped[date].bolsas += item.cantidadBolsas;
      grouped[date].peso += item.pesoTotal;
    });

    return grouped;
  }

  private groupByType(): { [type: string]: number } {
    const grouped: { [type: string]: number } = {};

    this.reportData.forEach(item => {
      const type = item.tipoGenerador || 'Sin tipo';
      if (!grouped[type]) {
        grouped[type] = 0;
      }
      grouped[type] += item.cantidadBolsas;
    });

    return grouped;
  }

  private groupByGenerator(): { [generator: string]: number } {
    const grouped: { [generator: string]: number } = {};

    this.reportData.forEach(item => {
      const generator = item.generador || 'Sin generador';
      if (!grouped[generator]) {
        grouped[generator] = 0;
      }
      grouped[generator] += item.cantidadBolsas;
    });

    return grouped;
  }

  private groupByZone(): { [zone: string]: number } {
    const grouped: { [zone: string]: number } = {};

    this.reportData.forEach(item => {
      const zone = item.zona || 'Sin zona';
      if (!grouped[zone]) {
        grouped[zone] = 0;
      }
      grouped[zone] += item.cantidadBolsas;
    });

    return grouped;
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
    if (this.topChart) {
      this.topChart.destroy();
    }
  }
}
