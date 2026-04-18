import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

// ===================================================
// INTERFACES PARA EL REPORTE DE ZONAS
// ===================================================

export interface ZoneReportDto {
  zona: string;
  totalSolicitudes: number;
  solicitudesPendientes: number;
  solicitudesCompletadas: number;
  totalBolsas: number;
  fechaUltimaSolicitud: string;
  promedioDiasProcesamiento: number;
  porcentajeCompletado: number;
}

export interface ZoneReportKpisDto {
  totalSolicitudesGlobal: number;
  zonaConMasSolicitudes: string;
  promedioSolicitudesPorZona: number;
  porcentajeCompletadoGeneral: number;
  totalBolsasRecolectadas: number;
  tiempoPromedioRespuesta: number;
}

export interface ZoneReportResponseDto {
  success: boolean;
  data: ZoneReportDto[];
  kpis: ZoneReportKpisDto;
  message?: string;
}

export interface ZoneReportFiltersDto {
  fechaInicio?: string;
  fechaFin?: string;
  estado?: string;
  zona?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ZoneReportService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8081/api/admin/reporte-zonas';

  /**
   * Obtiene el reporte completo de zonas con filtros
   */
  generateZoneReport(filters: ZoneReportFiltersDto): Observable<ZoneReportResponseDto> {
    let params = new HttpParams();

    if (filters.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }
    if (filters.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }
    if (filters.estado && filters.estado !== 'TODOS') {
      params = params.set('estado', filters.estado);
    }
    if (filters.zona && filters.zona !== 'TODAS') {
      params = params.set('zona', filters.zona);
    }

    return this.http.get<ZoneReportResponseDto>(this.apiUrl, { params });
  }

  /**
   * Obtiene solo los KPIs del reporte
   */
  getZoneKPIs(filters: ZoneReportFiltersDto): Observable<ZoneReportKpisDto> {
    let params = new HttpParams();

    if (filters.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }
    if (filters.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }
    if (filters.estado && filters.estado !== 'TODOS') {
      params = params.set('estado', filters.estado);
    }
    if (filters.zona && filters.zona !== 'TODAS') {
      params = params.set('zona', filters.zona);
    }

    return this.http.get<ZoneReportKpisDto>(`${this.apiUrl}/kpis`, { params });
  }

  /**
   * Exporta el reporte a Excel
   */
  exportToExcel(filters: ZoneReportFiltersDto): Observable<Blob> {
    let params = new HttpParams();

    if (filters.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }
    if (filters.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }
    if (filters.estado && filters.estado !== 'TODOS') {
      params = params.set('estado', filters.estado);
    }
    if (filters.zona && filters.zona !== 'TODAS') {
      params = params.set('zona', filters.zona);
    }

    return this.http.get(`${this.apiUrl}/export/excel`, {
      params,
      responseType: 'blob'
    });
  }

  /**
   * Exporta el reporte a PDF
   */
  exportToPDF(filters: ZoneReportFiltersDto): Observable<Blob> {
    let params = new HttpParams();

    if (filters.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }
    if (filters.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }
    if (filters.estado && filters.estado !== 'TODOS') {
      params = params.set('estado', filters.estado);
    }
    if (filters.zona && filters.zona !== 'TODAS') {
      params = params.set('zona', filters.zona);
    }

    return this.http.get(`${this.apiUrl}/export/pdf`, {
      params,
      responseType: 'blob'
    });
  }

  /**
   * Obtiene detalles específicos de una zona
   */
  getZoneDetails(zona: string, filters?: Partial<ZoneReportFiltersDto>): Observable<ZoneReportDto> {
    let params = new HttpParams().set('zona', zona);

    if (filters?.fechaInicio) {
      params = params.set('fechaInicio', filters.fechaInicio);
    }
    if (filters?.fechaFin) {
      params = params.set('fechaFin', filters.fechaFin);
    }
    if (filters?.estado && filters.estado !== 'TODOS') {
      params = params.set('estado', filters.estado);
    }

    return this.http.get<ZoneReportDto>(`${this.apiUrl}/detalle`, { params });
  }

  /**
   * Obtiene la lista de nombres de zonas disponibles
   */
  getAvailableZones(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/zonas/nombres`);
  }
}
