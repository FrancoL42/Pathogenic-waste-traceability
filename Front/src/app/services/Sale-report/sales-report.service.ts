import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

// Interfaces para el servicio
export interface SalesReportFiltersDto {
  fechaInicio?: string;
  fechaFin?: string;
  tipoGenerador?: string;
  zona?: string;
}

export interface SalesReportDto {
  generador: string;
  tipoGenerador: string;
  zona: string;
  totalVentas: number;
  cantidadBolsas: number;
  montoTotal: number;
  fechaUltimaVenta: string; // Se recibe como string desde el backend (JSON)
  promedioVentasPorMes: number;
  crecimientoMensual: number;
}

export interface SalesReportKPIsDto {
  totalVentasGlobal: number;
  generadorConMasVentas: string;
  promedioBolsasPorPeriodo: number;
  porcentajeTipoPublico: number;
  porcentajeTipoPrivado: number;
  tendenciaCrecimiento: number;
  montoTotalVentas: number;
}

export interface SalesReportResponseDto {
  success: boolean;
  message: string;
  data: SalesReportDto[];
  kpis: SalesReportKPIsDto;
}

@Injectable({
  providedIn: 'root'
})
export class SalesReportService {
  private readonly apiUrl = `http://localhost:8081/api/reports/sales`;

  constructor(private http: HttpClient) {}

  /**
   * Genera el reporte de ventas de bolsas
   */
  generateSalesReport(filters: SalesReportFiltersDto): Observable<SalesReportResponseDto> {
    return this.http.post<SalesReportResponseDto>(`${this.apiUrl}/generate`, filters);
  }

  /**
   * Genera el reporte usando parámetros GET (alternativo)
   */
  generateSalesReportGet(filters: SalesReportFiltersDto): Observable<SalesReportResponseDto> {
    let params = new HttpParams();

    if (filters.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }
    if (filters.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }
    if (filters.tipoGenerador) {
      params = params.set('tipoGenerador', filters.tipoGenerador);
    }
    if (filters.zona) {
      params = params.set('zona', filters.zona);
    }

    return this.http.get<SalesReportResponseDto>(`${this.apiUrl}/generate`, { params });
  }

  /**
   * Obtiene las zonas disponibles
   */
  getAvailableZones(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/zones`);
  }

  /**
   * Exporta el reporte a Excel
   */
  exportToExcel(filters: SalesReportFiltersDto): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/export/excel`, filters, {
      responseType: 'blob',
      headers: {
        'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      }
    });
  }

  /**
   * Exporta el reporte a Excel usando GET
   */
  exportToExcelGet(filters: SalesReportFiltersDto): Observable<Blob> {
    let params = new HttpParams();

    if (filters.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }
    if (filters.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }
    if (filters.tipoGenerador) {
      params = params.set('tipoGenerador', filters.tipoGenerador);
    }
    if (filters.zona) {
      params = params.set('zona', filters.zona);
    }

    return this.http.get(`${this.apiUrl}/export/excel`, {
      params,
      responseType: 'blob',
      headers: {
        'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      }
    });
  }

  /**
   * Exporta el reporte a PDF
   */
  exportToPDF(filters: SalesReportFiltersDto): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/export/pdf`, filters, {
      responseType: 'blob',
      headers: {
        'Accept': 'application/pdf'
      }
    });
  }

  /**
   * Exporta el reporte a PDF usando GET
   */
  exportToPDFGet(filters: SalesReportFiltersDto): Observable<Blob> {
    let params = new HttpParams();

    if (filters.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }
    if (filters.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }
    if (filters.tipoGenerador) {
      params = params.set('tipoGenerador', filters.tipoGenerador);
    }
    if (filters.zona) {
      params = params.set('zona', filters.zona);
    }

    return this.http.get(`${this.apiUrl}/export/pdf`, {
      params,
      responseType: 'blob',
      headers: {
        'Accept': 'application/pdf'
      }
    });
  }

  /**
   * Health check del servicio
   */
  healthCheck(): Observable<string> {
    return this.http.get(`${this.apiUrl}/health`, { responseType: 'text' });
  }

  /**
   * Descarga un archivo blob con el nombre especificado
   */
  downloadFile(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
}
