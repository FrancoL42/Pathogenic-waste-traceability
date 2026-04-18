import {inject, Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Generador} from "../../models/Generador";
import {Zona} from "../../models/Zona";
import {Vehiculo} from "../../models/Vehiculo";
import {Empleado} from "../../models/Empleado";
import {Pedido, Ruta} from "../../models/Pedido";
import {CreateRoadmapRequest} from "../Retiro/retiro.service";
import {StockInfo} from "../../models/Interfaces";

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly baseUrl = 'http://localhost:8081/admin';
  private readonly roadmapUrl = 'http://localhost:8081/roadmap'; // ✅ NUEVA URL
  private readonly http = inject(HttpClient);

  getZonas(): Observable<Zona[]> {
    return this.http.get<Zona[]>(`${this.baseUrl}/zones`);
  }

  getVehiculos(): Observable<Vehiculo[]> {
    return this.http.get<Vehiculo[]>(`${this.baseUrl}/vehicles`);
  }

  getEmpleados(): Observable<Empleado[]> {
    return this.http.get<Empleado[]>(`${this.baseUrl}/employees`);
  }

  getPedidosPorZona(zona: string): Observable<Pedido[]> {
    const url = `${this.baseUrl}/pedidos?zone=${encodeURIComponent(zona)}`;
    return this.http.get<Pedido[]>(url);
  }

  createRoadmap(request: CreateRoadmapRequest): Observable<any> {
    return this.http.post<any>(`${this.roadmapUrl}/create-from-orders`, request);
  }

  registrarVehiculo(vehiculo: { patent: string; type: string }): Observable<void> {
    return this.http.post<void>('http://localhost:8081/vehicle/register', vehiculo);
  }

  registrarEmpleado(empleado: { name: string, email: string }): Observable<Empleado> {
    console.log(empleado)
    return this.http.post<Empleado>('http://localhost:8081/employee/register', empleado);
  }

  getAssignedRequests(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>('/api/orders/assigned');
  }

  getBestRoute(requestId: number): Observable<Ruta> {
    return this.http.get<Ruta>(`/api/routes/best/${requestId}`);
  }

    aprobarSolicitud(generadorId: number | undefined): Observable<string> {
    return this.http.put<string>(
      `http://localhost:8081/Generator/aprobar?idGenerator=` + generadorId,
      null,
      { responseType: 'text' as 'json' }
    );
  }
  getStock(): Observable<StockInfo[]> {
    return this.http.get<StockInfo[]>(`${this.baseUrl}/stock`);
  }

  getLowStock(): Observable<StockInfo[]> {
    return this.http.get<StockInfo[]>(`${this.baseUrl}/stock/low`);
  }

  solicitarReposicion(bagId: number): Observable<string> {
    return this.http.post(`${this.baseUrl}/solicitar-reposicion/${bagId}`, {}, {
      responseType: 'text'
    });
  }

  incrementarStock(bagId: number, quantity: number): Observable<string> {
    return this.http.post(`${this.baseUrl}/stock/increment/${bagId}?quantity=${quantity}`, {}, {
      responseType: 'text'
    });
  }
}
