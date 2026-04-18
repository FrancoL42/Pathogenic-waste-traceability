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
import {DatePipe, NgForOf, NgIf, CurrencyPipe, DecimalPipe, SlicePipe} from "@angular/common";
import { MatProgressSpinner } from "@angular/material/progress-spinner";
import { MatProgressBar } from "@angular/material/progress-bar";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import {
  SalesReportDto,
  SalesReportFiltersDto, SalesReportKPIsDto,
  SalesReportService
} from "../../services/Sale-report/sales-report.service";

// Interfaces locales para el componente
export interface SalesReportData {
  generador: string;
  tipoGenerador: string;
  zona: string;
  totalVentas: number;
  cantidadBolsas: number;
  montoTotal: number;
  fechaUltimaVenta: string;
  promedioVentasPorMes: number;
  crecimientoMensual: number;
}

export interface SalesKPIs {
  totalVentasGlobal: number;
  generadorConMasVentas: string;
  promedioBolsasPorPeriodo: number;
  porcentajeTipoPublico: number;
  porcentajeTipoPrivado: number;
  tendenciaCrecimiento: number;
  montoTotalVentas: number;
}

@Component({
  selector: 'app-sales-report',
  templateUrl: './sales-report.component.html',
  standalone: true,
  imports: [
    // Angular Common
    NgIf,
    NgForOf,
    DatePipe,
    CurrencyPipe,
    DecimalPipe,
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
    SlicePipe
  ],
  styleUrls: ['./sales-report.component.css']
})
export class SalesReportComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild('topGeneratorsChart') topGeneratorsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('typeDistributionChart') typeDistributionChartRef!: ElementRef<HTMLCanvasElement>;

  // Servicios
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly salesReportService = inject(SalesReportService);

  // Formularios y datos
  filterForm: FormGroup;
  reportData: SalesReportData[] = [];
  availableZones: string[] = [];
  availableTypes: string[] = ['Público', 'Privado'];
  kpis: SalesKPIs = {
    totalVentasGlobal: 0,
    generadorConMasVentas: '',
    promedioBolsasPorPeriodo: 0,
    porcentajeTipoPublico: 0,
    porcentajeTipoPrivado: 0,
    tendenciaCrecimiento: 0,
    montoTotalVentas: 0
  };

  // Tabla
  dataSource = new MatTableDataSource<SalesReportData>();
  displayedColumns: string[] = [
    'generador',
    'tipoGenerador',
    'zona',
    'cantidadBolsas',
    'montoTotal',
    'promedioVentasPorMes',
    'crecimientoMensual',
    'fechaUltimaVenta'
  ];

  // Charts
  topGeneratorsChart: Chart | null = null;
  typeDistributionChart: Chart | null = null;

  // Estados
  isLoading = false;
  filtrosAplicados = false;

  constructor() {
    // Registrar componentes de Chart.js
    Chart.register(...registerables);

    this.filterForm = this.fb.group({
      fechaInicio: [''],
      fechaFin: [''],
      tipoGenerador: ['TODOS'],
      zona: ['TODAS']
    });

    // Establecer fechas por defecto (últimos 3 meses)
    const today = new Date();
    const threeMonthsAgo = new Date();
    threeMonthsAgo.setMonth(today.getMonth() - 3);

    this.filterForm.patchValue({
      fechaInicio: threeMonthsAgo.toISOString().split('T')[0],
      fechaFin: today.toISOString().split('T')[0]
    });
  }

  ngOnInit(): void {
    this.snackBar.dismiss();
    this.loadAvailableZones();
    console.log('SalesReportComponent inicializado');
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
    this.salesReportService.getAvailableZones().subscribe({
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
    console.log('Generando reporte de ventas...');
    this.snackBar.dismiss();
    this.isLoading = true;

    const filters: SalesReportFiltersDto = this.buildFilters();

    this.salesReportService.generateSalesReport(filters).subscribe({
      next: (response) => {
        if (response.success) {
          this.reportData = this.mapToLocalInterface(response.data);
          this.kpis = this.mapKPIsToLocalInterface(response.kpis);
          this.dataSource.data = this.reportData;
          this.updateCharts();
          this.filtrosAplicados = true;
          this.showSuccess('Reporte generado exitosamente');
        } else {
          this.showError(response.message || 'Error al generar el reporte');
          this.filtrosAplicados = false;
        }
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        this.showError('Error al generar el reporte: ' + (error.message || 'Error desconocido'));
        console.error('Error:', error);
        this.filtrosAplicados = false;
      }
    });
  }

  /**
   * Construye los filtros para enviar al backend
   */
  private buildFilters(): SalesReportFiltersDto {
    const formValue = this.filterForm.value;
    return {
      fechaInicio: formValue.fechaInicio || undefined,
      fechaFin: formValue.fechaFin || undefined,
      tipoGenerador: formValue.tipoGenerador === 'TODOS' ? undefined : formValue.tipoGenerador,
      zona: formValue.zona === 'TODAS' ? undefined : formValue.zona
    };
  }

  /**
   * Mapea la respuesta del backend a la interfaz local
   */
  private mapToLocalInterface(data: SalesReportDto[]): SalesReportData[] {
    return data.map(item => ({
      generador: item.generador,
      tipoGenerador: item.tipoGenerador,
      zona: item.zona,
      totalVentas: item.totalVentas,
      cantidadBolsas: item.cantidadBolsas,
      montoTotal: item.montoTotal,
      fechaUltimaVenta: item.fechaUltimaVenta,
      promedioVentasPorMes: item.promedioVentasPorMes,
      crecimientoMensual: item.crecimientoMensual
    }));
  }

  /**
   * Mapea los KPIs del backend a la interfaz local
   */
  private mapKPIsToLocalInterface(kpis: SalesReportKPIsDto): SalesKPIs {
    return {
      totalVentasGlobal: kpis.totalVentasGlobal,
      generadorConMasVentas: kpis.generadorConMasVentas,
      promedioBolsasPorPeriodo: kpis.promedioBolsasPorPeriodo,
      porcentajeTipoPublico: kpis.porcentajeTipoPublico,
      porcentajeTipoPrivado: kpis.porcentajeTipoPrivado,
      tendenciaCrecimiento: kpis.tendenciaCrecimiento,
      montoTotalVentas: kpis.montoTotalVentas
    };
  }

  updateCharts(): void {
    setTimeout(() => {
      this.createTopGeneratorsChart();
      this.createTypeDistributionChart();
    }, 100);
  }

  createTopGeneratorsChart(): void {
    if (this.topGeneratorsChart) {
      this.topGeneratorsChart.destroy();
    }

    const ctx = this.topGeneratorsChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    const topData = this.reportData
      .sort((a, b) => b.cantidadBolsas - a.cantidadBolsas)
      .slice(0, 10);

    this.topGeneratorsChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: topData.map(item => item.generador.length > 15 ?
          item.generador.substring(0, 15) + '...' : item.generador),
        datasets: [{
          label: 'Cantidad de Bolsas',
          data: topData.map(item => item.cantidadBolsas),
          backgroundColor: [
            '#0066CC', '#00A650', '#FF6600', '#00B5B8', '#6B46C1',
            '#8B5CF6', '#F59E0B', '#EF4444', '#10B981', '#3B82F6'
          ],
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
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

  createTypeDistributionChart(): void {
    if (this.typeDistributionChart) {
      this.typeDistributionChart.destroy();
    }

    const ctx = this.typeDistributionChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    this.typeDistributionChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Público', 'Privado'],
        datasets: [{
          data: [this.kpis.porcentajeTipoPublico, this.kpis.porcentajeTipoPrivado],
          backgroundColor: ['#0066CC', '#00A650'],
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

  clearFilters(): void {
    console.log('Limpiando filtros...');
    this.snackBar.dismiss();

    const today = new Date();
    const threeMonthsAgo = new Date();
    threeMonthsAgo.setMonth(today.getMonth() - 3);

    this.filterForm.reset({
      fechaInicio: threeMonthsAgo.toISOString().split('T')[0],
      fechaFin: today.toISOString().split('T')[0],
      tipoGenerador: 'TODOS',
      zona: 'TODAS'
    });
    this.filtrosAplicados = false;
  }

  /**
   * Exporta el reporte a Excel usando el servicio
   */
  exportToExcel(): void {
    const filters = this.buildFilters();

    this.salesReportService.exportToExcel(filters).subscribe({
      next: (blob) => {
        const filename = `reporte_ventas_bolsas_${new Date().toISOString().split('T')[0]}.xlsx`;
        this.salesReportService.downloadFile(blob, filename);
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

    this.salesReportService.exportToPDF(filters).subscribe({
      next: (blob) => {
        const filename = `reporte_ventas_bolsas_${new Date().toISOString().split('T')[0]}.pdf`;
        this.salesReportService.downloadFile(blob, filename);
        this.showSuccess('Reporte exportado exitosamente');
      },
      error: (error) => {
        if (error.status === 501) {
          this.showError('Funcionalidad de PDF en desarrollo');
        } else {
          this.showError('Error al exportar el reporte a PDF');
        }
        console.error('Error:', error);
      }
    });
  }

  getCrecimientoColor(crecimiento: number): string {
    if (crecimiento > 0) return 'success';
    if (crecimiento < 0) return 'warning';
    return 'info';
  }

  getCrecimientoIcon(crecimiento: number): string {
    if (crecimiento > 0) return 'trending_up';
    if (crecimiento < 0) return 'trending_down';
    return 'trending_flat';
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

    if (this.topGeneratorsChart) {
      this.topGeneratorsChart.destroy();
    }
    if (this.typeDistributionChart) {
      this.typeDistributionChart.destroy();
    }
  }
}
