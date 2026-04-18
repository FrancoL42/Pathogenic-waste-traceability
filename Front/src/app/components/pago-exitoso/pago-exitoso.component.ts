import { Component } from '@angular/core';
import {MatCard, MatCardModule} from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import {MatIcon} from '@angular/material/icon';
import { MatIconModule } from '@angular/material/icon';
import {GeneratorService} from "../../services/Generador/generator.service";

@Component({
  selector: 'app-pago-exitoso',
  standalone: true,
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './pago-exitoso.component.html',
  styleUrl: './pago-exitoso.component.css'
})
export class PagoExitosoComponent {
  confirmando = false;
  ventaConfirmada = false;

  constructor(private generatorService: GeneratorService) {}

  ngOnInit() {
    this.confirmarVenta();
  }

  confirmarVenta() {
    const saleId = localStorage.getItem('saleId');
    const generatorId = localStorage.getItem('generatorId');

    if (saleId && generatorId) {
      this.confirmando = true;

      this.generatorService.confirmSale(Number(saleId), Number(generatorId))
        .subscribe({
          next: (response) => {
            console.log('Venta confirmada:', response);
            this.confirmando = false;
            this.ventaConfirmada = true;

            // Limpiar localStorage
            localStorage.removeItem('saleId');
            localStorage.removeItem('generatorId');
          },
          error: (error) => {
            console.error('Error al confirmar venta:', error);
            this.confirmando = false;
          }
        });
    }
  }

  volver() {
    window.location.href = '/';
  }
}
