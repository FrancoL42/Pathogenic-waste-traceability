import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class TreatmentService {

  private baseUrl = 'http://localhost:8081/api/treatment';

  constructor(private http: HttpClient) {}

  getAvailableSeals(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/seals/available`);
  }

  getContainers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/containers`);
  }

  createContainer(pesoMaximo: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/containers?pesoMaximo=${pesoMaximo}`, {});
  }

  addSealsToContainer(containerId: number, sealIds: number[]): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/containers/${containerId}/add-seals`, sealIds);
  }

  removeSealsFromContainer(containerId: number, sealIds: number[]): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/containers/${containerId}/remove-seals`, sealIds);
  }

  closeContainer(containerId: number): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/containers/${containerId}/close`, {});
  }

  // 🆕 MÉTODO RENOMBRADO para procesar tratamiento y reutilizar contenedor
  processContainerTreatment(containerId: number): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/containers/${containerId}/open-treated`, {});
  }

  // 🗑️ ELIMINAR: openTreatedContainer - ya no se usa
  // openTreatedContainer(containerId: number): Observable<any> {
  //   return this.http.put<any>(`${this.baseUrl}/containers/${containerId}/open-treated`, {});
  // }

  weightSeal(sealId: number, peso: number): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/seals/${sealId}/weight?peso=${peso}`, {});
  }

  weightSealsLote(pesajes: any[]): Observable<any> {
    return this.http.put(`${this.baseUrl}/seals/weight-batch`, pesajes);
  }
}
