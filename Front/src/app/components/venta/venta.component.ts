import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { GeneratorService } from "../../services/Generador/generator.service";
import { CommonModule, CurrencyPipe } from "@angular/common";
import { Router } from "@angular/router";
import { HttpHeaders } from "@angular/common/http";

// Declarar MercadoPago como variable global
declare var MercadoPago: any;

@Component({
  selector: 'app-venta',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CurrencyPipe,
    CommonModule
  ],
  templateUrl: './venta.component.html',
  styleUrl: './venta.component.css'
})
export class VentaComponent {
  formulario!: FormGroup;
  generatorService: GeneratorService = inject(GeneratorService);
  private mp: any;
  router: Router = inject(Router);

  constructor(private fb: FormBuilder) {
    this.formulario = this.fb.group({
      idBolsa: ['', Validators.required],
      cantidad: [1, [Validators.required, Validators.min(1)]]
    });
  }

  bolsas: any[] = [];
  precioUnitario: number = 0;
  totalCompra: number = 0;

  ngOnInit(): void {
    // Inicializar MercadoPago
    this.initMercadoPago();

    this.cargarBolsas();

    this.formulario.valueChanges.subscribe(() => {
      this.actualizarPrecio();
    });
  }

  initMercadoPago(): void {
    try {
      // USAR tu nueva Public Key de la cuenta de prueba
      this.mp = new MercadoPago('APP_USR-4514190446946115-052622-8aaadcdfda86331dbdcf80d309eb92dc-2446194113', {
        locale: 'es-AR'
      });
      console.log('MercadoPago inicializado correctamente con cuenta de prueba');
    } catch (error) {
      console.error('Error al inicializar MercadoPago:', error);
    }
  }

  cargarBolsas(): void {
    this.generatorService.getBolsas().subscribe({
      next: (data) => {
        this.bolsas = data;
        console.log(data);
      },
      error: (err) => {
        console.error('Error al cargar bolsas', err);
      }
    });
  }

  actualizarPrecio(): void {
    const form = this.formulario.value;
    const bolsaSeleccionada = this.bolsas[form.idBolsa];
    this.precioUnitario = bolsaSeleccionada?.price || 0;
    this.totalCompra = this.precioUnitario * (form.cantidad || 0);
  }

  volverAlDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  procesarCompra(): void {
    if (this.formulario.invalid) {
      return alert('Complete los datos correctamente');
    }

    const { idBolsa, cantidad } = this.formulario.value;
    const bolsa = this.bolsas[idBolsa];
    if (!bolsa) {
      return alert('Seleccione una bolsa válida');
    }

    const title = 'Bolsa ' + bolsa.size;
    const unitPrice = this.precioUnitario;
    const quantity = cantidad;

    const token = localStorage.getItem('token');
    if (!token) {
      return alert('No autenticado');
    }

    this.generatorService.realizarCompra({ title, unitPrice, quantity })
      .subscribe({
        next: (response) => {
          if (response.initPoint) {
            const saleId = response.saleId;
            const generatorId = response.generatorId;

            // Mostrar notificación
            this.mostrarNotificacionPago();

            // Abrir ventana de pago
            const ventanaPago = window.open(response.initPoint, '_blank');

            // Detectar cuando se cierre y confirmar automáticamente
            const checkClosed = setInterval(() => {
              if (ventanaPago?.closed) {
                clearInterval(checkClosed);

                // Confirmar automáticamente la última venta
                this.generatorService.confirmarUltimaVenta(generatorId);
              }
            }, 1000);
          }
        }, error: (error) => {
          console.error('Error:', error);
          if (error.error && error.error.includes('precintos disponibles')) {
            alert('Error: No hay suficientes precintos disponibles para esta cantidad.');
          } else {
            alert('Error al generar la preferencia de pago');
          }
        }
      });
  }

  private mostrarNotificacionPago() {
    // Crear notificación elegante con estilo Veolia
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed; top: 20px; right: 20px;
        background: linear-gradient(135deg, #0066CC, #004A99);
        color: white;
        padding: 20px; border-radius: 16px;
        z-index: 9999;
        box-shadow: 0 8px 32px rgba(0, 102, 204, 0.3);
        max-width: 320px;
        border: 1px solid rgba(255, 255, 255, 0.2);
    `;
    notification.innerHTML = `
        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">
            <span style="font-size: 24px;">💳</span>
            <h3 style="margin: 0; font-size: 18px; font-weight: 600;">Procesando Pago</h3>
        </div>
        <p style="margin: 0 0 8px 0; line-height: 1.4;">Complete su pago en la nueva ventana.</p>
        <p style="margin: 0; font-size: 14px; opacity: 0.9; line-height: 1.4;">
            La confirmación será automática al cerrar la ventana.
        </p>
    `;

    document.body.appendChild(notification);

    // Remover después de 8 segundos
    setTimeout(() => notification.remove(), 8000);
  }
}
