// terms-modal.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatButton } from '@angular/material/button';

@Component({
  selector: 'app-terms-modal',
  standalone: true,
  imports: [CommonModule, MatIcon, MatButton],
  template: `
    <div class="terms-modal">
      <!-- Header del Modal -->
      <div class="modal-header">
        <div class="modal-title">
          <mat-icon class="title-icon">gavel</mat-icon>
          <h2>Términos y Condiciones</h2>
        </div>
        <button class="close-button" (click)="closeModal()" type="button">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Contenido del Modal -->
      <div class="modal-content">
        <div class="terms-content">

          <div class="company-info">
            <h3>AESA MISIONES S.A.</h3>
            <p><strong>CUIT:</strong> 30-70714831-0</p>
            <p><strong>Fecha de última actualización:</strong> Junio 2025</p>
          </div>

          <section class="terms-section">
            <h3>1. ACEPTACIÓN DE LOS TÉRMINOS</h3>
            <p>Al acceder y utilizar el Sistema de Gestión de Residuos Patogénicos (en adelante "el Sistema"), el usuario acepta quedar obligado por estos Términos y Condiciones. Si no está de acuerdo con alguna parte de estos términos, no debe utilizar el Sistema.</p>
          </section>

          <section class="terms-section">
            <h3>2. DESCRIPCIÓN DEL SERVICIO</h3>
            <h4>2.1 Objetivo del Sistema</h4>
            <p>El Sistema brinda información clara y oportuna para la correcta trazabilidad de los residuos patogénicos generados en la provincia de Misiones, desde que el generador solicita adhesión al servicio hasta la emisión del certificado de tratamiento.</p>

            <h4>2.2 Usuarios Elegibles</h4>
            <p>Pueden acceder al Sistema:</p>
            <ul>
              <li>Municipios de la provincia de Misiones</li>
              <li>Hospitales públicos y privados</li>
              <li>Clínicas veterinarias</li>
              <li>Clínicas odontológicas</li>
              <li>Laboratorios de análisis clínicos</li>
              <li>Farmacias</li>
              <li>Centros de investigación</li>
              <li>Gabinetes de enfermería</li>
              <li>Cualquier persona física o jurídica que genere residuos patológicos</li>
            </ul>
          </section>

          <section class="terms-section">
            <h3>3. REGISTRO Y ADHESIÓN AL SISTEMA</h3>
            <h4>3.1 Proceso de Adhesión</h4>
            <p>Para adherirse al Sistema, el generador debe:</p>
            <ul>
              <li>Enviar solicitud formal con datos completos de la empresa/institución</li>
              <li>Proporcionar información fiscal y de contacto actualizada</li>
              <li>Firmar el acuerdo comercial correspondiente</li>
              <li>Cumplir con los requisitos de habilitación municipal cuando corresponda</li>
            </ul>

            <h4>3.2 Verificación de Datos</h4>
            <p>AESA Misiones S.A. se reserva el derecho de verificar toda la información proporcionada y puede rechazar solicitudes que no cumplan con los requisitos establecidos.</p>
          </section>

          <section class="terms-section">
            <h3>4. USO DEL SISTEMA</h3>
            <h4>4.1 Acceso a la Plataforma</h4>
            <p>Cada usuario autorizado recibirá credenciales únicas e intransferibles para acceder al Sistema. Es responsabilidad del usuario mantener la confidencialidad de sus credenciales.</p>

            <h4>4.2 Funcionalidades Disponibles</h4>
            <p>El Sistema permite:</p>
            <ul>
              <li>Solicitar compra de bolsas y precintos</li>
              <li>Registrar solicitudes de retiro de residuos</li>
              <li>Consultar hojas de ruta asignadas</li>
              <li>Hacer seguimiento de la trazabilidad mediante códigos QR</li>
              <li>Descargar certificados de tratamiento</li>
              <li>Acceder al historial de operaciones</li>
            </ul>

            <h4>4.3 Uso Adecuado</h4>
            <p>Los usuarios se comprometen a:</p>
            <ul>
              <li>Utilizar el Sistema únicamente para fines relacionados con la gestión de residuos patogénicos</li>
              <li>Proporcionar información veraz y actualizada</li>
              <li>No intentar acceder a información de otros usuarios</li>
              <li>Reportar cualquier irregularidad o mal funcionamiento</li>
            </ul>
          </section>

          <section class="terms-section">
            <h3>5. ADQUISICIÓN DE BOLSAS Y PRECINTOS</h3>
            <h4>5.1 Tipos de Bolsas</h4>
            <ul>
              <li><strong>Bolsas grandes:</strong> 60 x 82 cm, capacidad máxima 5 kg</li>
              <li><strong>Bolsas chicas:</strong> 50 x 70 cm, capacidad máxima 2,5 kg</li>
              <li>Color rojo, 120 micrones de espesor</li>
              <li>Logo AESA Misiones S.A. según tipo de usuario</li>
            </ul>

            <h4>5.2 Precios y Facturación</h4>
            <p>Los precios vigentes se actualizan periódicamente. La facturación se genera al momento de la compra. Las condiciones de pago son:</p>
            <ul>
              <li>Plazo máximo: 30 días corridos desde la emisión</li>
              <li>Cuenta corriente Banco Macro S.A. Nro. 300100080033046</li>
              <li>CBU: 2850001030000800330461</li>
            </ul>

            <h4>5.3 Mora y Suspensión</h4>
            <p>El incumplimiento en los pagos genera:</p>
            <ul>
              <li>Mora automática</li>
              <li>Intereses moratorios del 1,5 veces la tasa del Banco Nación</li>
              <li>Posible suspensión del servicio</li>
              <li>Facultad de resolución del acuerdo</li>
            </ul>
          </section>

          <section class="terms-section">
            <h3>6. TRAZABILIDAD Y CONTROL</h3>
            <h4>6.1 Sistema de Códigos QR</h4>
            <p>Cada precinto cuenta con código QR correlativo que permite:</p>
            <ul>
              <li>Identificación del generador</li>
              <li>Seguimiento completo del proceso</li>
              <li>Control en cada etapa del tratamiento</li>
              <li>Emisión automática de certificados</li>
            </ul>

            <h4>6.2 Responsabilidades del Generador</h4>
            <ul>
              <li>Acondicionar residuos según normativas</li>
              <li>Utilizar únicamente bolsas provistas por AESA</li>
              <li>Permitir inspecciones de control</li>
              <li>Firmar documentación de retiro</li>
            </ul>
          </section>

          <section class="terms-section">
            <h3>7. PRIVACIDAD Y PROTECCIÓN DE DATOS</h3>
            <h4>7.1 Recopilación de Información</h4>
            <p>AESA Misiones S.A. recopila información necesaria para la prestación del servicio, incluyendo datos de identificación, ubicación, facturación y trazabilidad de residuos.</p>

            <h4>7.2 Uso de la Información</h4>
            <p>Los datos se utilizan exclusivamente para:</p>
            <ul>
              <li>Prestación del servicio contratado</li>
              <li>Cumplimiento de obligaciones legales</li>
              <li>Mejora del Sistema</li>
              <li>Comunicaciones relacionadas con el servicio</li>
            </ul>

            <h4>7.3 Seguridad</h4>
            <p>Implementamos medidas de seguridad apropiadas para proteger la información contra acceso no autorizado, alteración, divulgación o destrucción.</p>
          </section>

          <section class="terms-section">
            <h3>8. LIMITACIÓN DE RESPONSABILIDAD</h3>
            <h4>8.1 Límite de Responsabilidad</h4>
            <p>La responsabilidad total de AESA Misiones S.A. está limitada al 10% del precio efectivamente facturado y percibido por la prestación del servicio.</p>

            <h4>8.2 Exclusiones</h4>
            <p>AESA Misiones S.A. no será responsable por:</p>
            <ul>
              <li>Lucro cesante o pérdidas indirectas</li>
              <li>Daños por incumplimiento del generador</li>
              <li>Interrupciones del Sistema por causas de fuerza mayor</li>
              <li>Pérdidas derivadas del uso inadecuado del Sistema</li>
            </ul>
          </section>

          <section class="terms-section">
            <h3>9. DISPONIBILIDAD DEL SISTEMA</h3>
            <h4>9.1 Tiempo de Operación</h4>
            <p>Nos esforzamos por mantener el Sistema disponible 24/7, pero no garantizamos disponibilidad ininterrumpida debido a:</p>
            <ul>
              <li>Mantenimientos programados</li>
              <li>Actualizaciones del sistema</li>
              <li>Circunstancias imprevistas</li>
            </ul>

            <h4>9.2 Soporte Técnico</h4>
            <p>Contacto para soporte:</p>
            <ul>
              <li>WhatsApp: +54 9 376 5172011</li>
              <li>Horario de atención: Lunes a Viernes 8:00 - 18:00</li>
            </ul>
          </section>

          <section class="terms-section">
            <h3>10. MODIFICACIONES</h3>
            <h4>10.1 Términos y Condiciones</h4>
            <p>AESA Misiones S.A. puede modificar estos términos en cualquier momento mediante preaviso de 30 días. El uso continuado del Sistema implica aceptación de las modificaciones.</p>

            <h4>10.2 Funcionalidades del Sistema</h4>
            <p>Las funcionalidades pueden ser modificadas, agregadas o eliminadas para mejorar el servicio, con la debida notificación a los usuarios.</p>
          </section>

          <section class="terms-section">
            <h3>11. VIGENCIA Y RESCISIÓN</h3>
            <h4>11.1 Duración</h4>
            <p>El acceso al Sistema está sujeto a la vigencia del acuerdo comercial correspondiente.</p>

            <h4>11.2 Causas de Rescisión</h4>
            <p>AESA Misiones S.A. puede suspender o cancelar el acceso por:</p>
            <ul>
              <li>Incumplimiento de estos términos</li>
              <li>Actividades que afecten el prestigio de la empresa</li>
              <li>Uso fraudulento o inadecuado del Sistema</li>
              <li>Mora en los pagos</li>
            </ul>
          </section>

          <section class="terms-section">
            <h3>12. CUMPLIMIENTO LEGAL</h3>
            <p>Los usuarios deben cumplir con:</p>
            <ul>
              <li>Ley Nacional 24.051 de Residuos Peligrosos</li>
              <li>Normativas provinciales de Misiones</li>
              <li>Resoluciones municipales aplicables</li>
              <li>Disposiciones de autoridades sanitarias</li>
            </ul>
          </section>

          <section class="terms-section">
            <h3>13. JURISDICCIÓN</h3>
            <p>Cualquier controversia será resuelta por los Tribunales Ordinarios de Posadas, provincia de Misiones, con exclusión de cualquier otro fuero o jurisdicción.</p>
          </section>

          <section class="terms-section">
            <h3>14. CONTACTO</h3>
            <div class="contact-info">
              <p><strong>AESA MISIONES S.A.</strong></p>
              <p>Teléfono: +54 9 376 5172011</p>
              <p>WhatsApp: +54 9 376 5172011</p>
            </div>
          </section>

          <div class="acceptance-notice">
            <p><strong>Al utilizar el Sistema de Gestión de Residuos Patogénicos, usted acepta cumplir con estos Términos y Condiciones en su totalidad.</strong></p>
          </div>
        </div>
      </div>

      <!-- Footer del Modal -->
      <div class="modal-footer">
        <button mat-button class="btn-accept" (click)="acceptAndClose()" type="button">
          <mat-icon>check_circle</mat-icon>
          He leído y acepto
        </button>
        <button mat-button class="btn-cancel" (click)="closeModal()" type="button">
          Cerrar
        </button>
      </div>
    </div>
  `,
  styles: [`
    .terms-modal {
      width: 100%;
      max-width: 90vw;
      max-height: 85vh;
      display: flex;
      flex-direction: column;
      background: white;
      border-radius: 16px;
      overflow: hidden;
    }

    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1.5rem 2rem;
      background: linear-gradient(135deg, var(--veolia-blue-primary, #0066CC), var(--veolia-blue-dark, #004A99));
      color: white;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .modal-title {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .modal-title h2 {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 600;
    }

    .title-icon {
      background: rgba(255, 255, 255, 0.2);
      padding: 0.5rem;
      border-radius: 8px;
      font-size: 1.5rem;
    }

    .close-button {
      background: rgba(255, 255, 255, 0.1);
      border: none;
      border-radius: 50%;
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: background-color 0.3s ease;
      color: white;
    }

    .close-button:hover {
      background: rgba(255, 255, 255, 0.2);
    }

    .modal-content {
      flex: 1;
      overflow-y: auto;
      padding: 0;
      max-height: calc(85vh - 140px);
    }

    .terms-content {
      padding: 2rem;
      line-height: 1.6;
      color: var(--veolia-gray-800, #1F2937);
    }

    .company-info {
      background: var(--veolia-blue-light, #E6F3FF);
      padding: 1.5rem;
      border-radius: 12px;
      margin-bottom: 2rem;
      border-left: 4px solid var(--veolia-blue-primary, #0066CC);
    }

    .company-info h3 {
      margin: 0 0 0.5rem 0;
      color: var(--veolia-blue-primary, #0066CC);
      font-size: 1.25rem;
      font-weight: 700;
    }

    .company-info p {
      margin: 0.25rem 0;
      font-size: 0.95rem;
    }

    .terms-section {
      margin-bottom: 2rem;
      padding-bottom: 1.5rem;
      border-bottom: 1px solid var(--veolia-gray-200, #DEE2E6);
    }

    .terms-section:last-child {
      border-bottom: none;
      margin-bottom: 0;
    }

    .terms-section h3 {
      color: var(--veolia-blue-primary, #0066CC);
      font-size: 1.1rem;
      font-weight: 700;
      margin: 0 0 1rem 0;
      padding-bottom: 0.5rem;
      border-bottom: 2px solid var(--veolia-green-primary, #00A650);
    }

    .terms-section h4 {
      color: var(--veolia-gray-700, #343A40);
      font-size: 1rem;
      font-weight: 600;
      margin: 1.5rem 0 0.75rem 0;
    }

    .terms-section p {
      margin: 0.75rem 0;
      text-align: justify;
    }

    .terms-section ul {
      margin: 0.75rem 0;
      padding-left: 1.5rem;
    }

    .terms-section li {
      margin: 0.5rem 0;
      text-align: justify;
    }

    .contact-info {
      background: var(--veolia-green-light, #E6F7ED);
      padding: 1rem;
      border-radius: 8px;
      border-left: 4px solid var(--veolia-green-primary, #00A650);
    }

    .contact-info p {
      margin: 0.25rem 0;
    }

    .acceptance-notice {
      background: var(--veolia-gray-50, #F8F9FA);
      padding: 1.5rem;
      border-radius: 12px;
      margin-top: 2rem;
      border: 2px solid var(--veolia-orange, #FF6600);
      text-align: center;
    }

    .acceptance-notice p {
      margin: 0;
      font-weight: 600;
      color: var(--veolia-orange, #FF6600);
    }

    .modal-footer {
      padding: 1.5rem 2rem;
      background: var(--veolia-gray-50, #F8F9FA);
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      border-top: 1px solid var(--veolia-gray-200, #DEE2E6);
    }

    .btn-accept {
      background: linear-gradient(135deg, var(--veolia-green-primary, #00A650), var(--veolia-green-dark, #007A3D)) !important;
      color: white !important;
      padding: 0.75rem 1.5rem !important;
      border-radius: 8px !important;
      font-weight: 600 !important;
      display: flex !important;
      align-items: center !important;
      gap: 0.5rem !important;
      border: none !important;
      cursor: pointer !important;
      transition: all 0.3s ease !important;
    }

    .btn-accept:hover {
      background: linear-gradient(135deg, var(--veolia-green-dark, #007A3D), #005c2a) !important;
      transform: translateY(-1px) !important;
    }

    .btn-cancel {
      background: var(--veolia-gray-200, #DEE2E6) !important;
      color: var(--veolia-gray-700, #343A40) !important;
      padding: 0.75rem 1.5rem !important;
      border-radius: 8px !important;
      font-weight: 500 !important;
      border: none !important;
      cursor: pointer !important;
      transition: all 0.3s ease !important;
    }

    .btn-cancel:hover {
      background: var(--veolia-gray-300, #CED4DA) !important;
    }

    /* Scroll personalizado */
    .modal-content::-webkit-scrollbar {
      width: 8px;
    }

    .modal-content::-webkit-scrollbar-track {
      background: var(--veolia-gray-100, #E9ECEF);
      border-radius: 4px;
    }

    .modal-content::-webkit-scrollbar-thumb {
      background: var(--veolia-gray-400, #ADB5BD);
      border-radius: 4px;
    }

    .modal-content::-webkit-scrollbar-thumb:hover {
      background: var(--veolia-gray-500, #6C757D);
    }

    /* Responsive */
    @media (max-width: 768px) {
      .terms-modal {
        max-width: 95vw;
        max-height: 90vh;
      }

      .modal-header {
        padding: 1rem 1.5rem;
      }

      .modal-title h2 {
        font-size: 1.25rem;
      }

      .terms-content {
        padding: 1.5rem;
      }

      .modal-footer {
        padding: 1rem 1.5rem;
        flex-direction: column;
      }

      .btn-accept,
      .btn-cancel {
        width: 100% !important;
        justify-content: center !important;
      }
    }
  `]
})
export class TermsModalComponent {
  constructor(
    private dialogRef: MatDialogRef<TermsModalComponent>
  ) {}

  closeModal(): void {
    this.dialogRef.close(false);
  }

  acceptAndClose(): void {
    this.dialogRef.close(true);
  }
}
