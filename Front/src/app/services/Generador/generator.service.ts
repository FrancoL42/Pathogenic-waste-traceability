import {inject, Injectable} from '@angular/core';
import {map, Observable, tap} from "rxjs";
import {HttpClient, HttpHeaders, HttpParams} from "@angular/common/http";
import {Generador} from "../../models/Generador";
import {Bolsa} from "../../models/Bolsa";
import {HistorialVentas} from "../../models/historialVentas";

@Injectable({
  providedIn: 'root'
})
export class GeneratorService {
  private apiUrl = 'http://localhost:8081/Generator';
  private readonly http = inject(HttpClient);
  constructor() {}

  registerClient(generador: Generador): Observable<Generador> {

    //return this.http.post<Generador>('http://localhost:8080/Generator', generador);
    return this.http.post<Generador>('http://localhost:8081/Generator/Register', generador);
  }
  getGeneradoresActivos(): Observable<Generador[]> {
    return this.http.get<Generador[]>('http://localhost:8081/Generator/activos\n');
  }
  getGeneradoresPendientes(): Observable<Generador[]> {
    return this.http.get<Generador[]>('http://localhost:8081/Generator/pendientes');
  }
  getHistorialCompras(): Observable<HistorialVentas[]> {
    return this.http.get<HistorialVentas[]>('http://localhost:8081/Sale/historial-ventas?id=6\n');
  }
  public confirmarUltimaVenta(generatorId: number) {
    const token = localStorage.getItem('token')!;
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    this.http.post(`http://localhost:8081/Sale/confirm-last-sale?generatorId=${generatorId}`, {}, { headers, responseType: 'text' })
      .subscribe({
        next: (response) => {
          console.log('Venta confirmada:', response);
          setTimeout(() => {
            window.location.href = '/pago-exitoso';
          }, 1000);
        },
        error: (error) => {
          console.error('Error confirmando venta:', error);
          // Aún así redirigir a éxito
          setTimeout(() => {
            window.location.href = '/pago-exitoso';
          }, 1000);
        }
      });
  }
  realizarCompra(data: { title: string, unitPrice: number, quantity: number }) {
    const token = localStorage.getItem('token')!;
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    const params = new HttpParams()
      .set('title', data.title)
      .set('unitPrice', data.unitPrice.toString())
      .set('quantity', data.quantity.toString());

    return this.http
      .post<any>('http://localhost:8081/Sale/crear-preferencia', null, { params, headers })
      .pipe(tap(pref => console.log('Preferencia MP:', pref)));
  }
  getBolsas(): Observable<Bolsa[]> {
    return this.http.get<Bolsa[]>('http://localhost:8081/Bag');
  }
  confirmSale(saleId: number, generatorId: number) {
    const token = localStorage.getItem('token')!;
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    const body = {
      saleId: saleId,
      generatorId: generatorId
    };

    return this.http.post('http://localhost:8081/Sale/confirm', body, { headers });
  }
}
