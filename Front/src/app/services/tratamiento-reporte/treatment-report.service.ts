import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

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

@Injectable({
  providedIn: 'root'
})
export class TreatmentReportService {
  private readonly baseUrl = 'http://localhost:8081/api/reports/treatments';
  private readonly http = inject(HttpClient);

  /**
   * Genera el reporte de tratamientos con los filtros especificados
   */
  generateTreatmentReport(filters: TreatmentReportFilters): Observable<TreatmentReportResponse> {
    let params = new HttpParams();

    if (filters.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }

    if (filters.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }

    if (filters.tipoGenerador && filters.tipoGenerador !== 'TODOS') {
      params = params.set('tipoGenerador', filters.tipoGenerador);
    }

    if (filters.zona && filters.zona !== 'TODAS') {
      params = params.set('zona', filters.zona);
    }

    console.log('Enviando petición con parámetros:', params.toString());

    return this.http.get<TreatmentReportResponse>(`${this.baseUrl}/generate`, { params });
  }

  /**
   * Obtiene la lista de zonas disponibles
   */
  getAvailableZones(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/zones`);
  }

  /**
   * Exporta el reporte a Excel
   */
  exportToExcel(filters: TreatmentReportFilters): Observable<Blob> {
    let params = new HttpParams();

    if (filters.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }

    if (filters.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }

    if (filters.tipoGenerador && filters.tipoGenerador !== 'TODOS') {
      params = params.set('tipoGenerador', filters.tipoGenerador);
    }

    if (filters.zona && filters.zona !== 'TODAS') {
      params = params.set('zona', filters.zona);
    }

    return this.http.get(`${this.baseUrl}/export/excel`, {
      params,
      responseType: 'blob'
    });
  }

  /**
   * Exporta el reporte a PDF
   */
  exportToPDF(filters: TreatmentReportFilters): Observable<Blob> {
    let params = new HttpParams();

    if (filters.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }

    if (filters.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }

    if (filters.tipoGenerador && filters.tipoGenerador !== 'TODOS') {
      params = params.set('tipoGenerador', filters.tipoGenerador);
    }

    if (filters.zona && filters.zona !== 'TODAS') {
      params = params.set('zona', filters.zona);
    }

    return this.http.get(`${this.baseUrl}/export/pdf`, {
      params,
      responseType: 'blob'
    });
  }
}
