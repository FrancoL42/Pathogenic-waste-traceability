import {inject, Injectable} from '@angular/core';
import {roadmap} from "../../models/roadmap";
import {Observable} from "rxjs";
import {HttpClient} from "@angular/common/http";
export interface CreateRoadmapRequest {
  zone: string;
  employee: string;
  collectDate: string;
  exitHour: string;
  selectedOrderIds: number[];
  vehicleId?: number;
}

export interface RoadmapDto {
  roadmapId: number;
  date: string;
  zone: string;
  collectDate: string;
  employee: string;
  initialKm?: number;
  finalKm?: number;
  exitHour: string;
  returnHour?: string;
  collectHour?: string;
  countBags?: number;
  details: RoadmapDetailDto[];
  state: string;
}

export interface RoadmapDetailDto {
  id: number;
  generatorId: number;
  generatorName: string;
  generatorAddress: string;
  latitude: number;
  longitude: number;
  generatorType: string;
  sealNumber?: string;
}

@Injectable({
  providedIn: 'root'
})
export class RetiroService {

  private apiUrl = 'http://localhost:8081/roadmap'; // Reemplaza con tu URL real

  private readonly http = inject(HttpClient);

  solicitarRetiro(dto: roadmap): Observable<roadmap> {
    return this.http.post<roadmap>(`${this.apiUrl}/crear`, dto);
  }
  createRoadmap(request: CreateRoadmapRequest): Observable<RoadmapDto> {
    return this.http.post<RoadmapDto>(`${this.apiUrl}/create`, request);
  }

  getAllRoadmaps(): Observable<RoadmapDto[]> {
    return this.http.get<RoadmapDto[]>(this.apiUrl);
  }

  getRoadmapById(id: number): Observable<RoadmapDto> {
    return this.http.get<RoadmapDto>(`${this.apiUrl}/${id}`);
  }

  updateRoadmap(id: number, roadmap: Partial<RoadmapDto>): Observable<RoadmapDto> {
    return this.http.put<RoadmapDto>(`${this.apiUrl}/${id}`, roadmap);
  }

}
