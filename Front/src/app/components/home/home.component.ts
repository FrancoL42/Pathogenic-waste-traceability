import {Component, inject} from '@angular/core';
import {MatToolbar, MatToolbarModule} from "@angular/material/toolbar";
import {MatCard, MatCardActions, MatCardContent, MatCardHeader, MatCardModule} from "@angular/material/card";
import {MatIconModule} from "@angular/material/icon";
import {MatButtonModule} from "@angular/material/button";
import {Router} from "@angular/router";
import {CommonModule} from "@angular/common";

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    MatToolbar,
    MatCard,
    MatCardHeader,
    MatCardContent,
    MatCardActions,
    MatCardModule,
    MatButtonModule,
    MatToolbarModule,
    MatIconModule,
    CommonModule
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  router = inject(Router);

  // Estado para controlar qué FAQ están abiertas
  faqOpen: { [key: number]: boolean } = {
    1: false,
    2: false,
    3: false,
    4: false,
    5: false,
    6: false
  };

  irADashboard() {
    this.router.navigate(['/login']);
  }

  irARegistro() {
    this.router.navigate(['/alta']);
  }

  irAContacto() {
    // Aquí puedes navegar a una página de contacto o abrir un modal
    console.log('Navegando a contacto...');
    // this.router.navigate(['/contacto']);
  }

  masInfo() {
    window.location.href = 'https://www.google.com/search?sca_esv=b9b1559b3529293d&rlz=1CAUHMT_enAR1145&sxsrf=AE3TifPK2ZwKgezhvnIe2-MFtGMLVZLFKg:1748643608942&q=tratamiento+de+residuos+patog%C3%A9nicos&udm=7&fbs=AIIjpHx4nJjfGojPVHhEACUHPiMQht6_BFq6vBIoFFRK7qchKBv8IM7dq8CEqHDU3BN7lbmYnvYQ6rIhpD6d6bj_VyqCDVICi0aYslRFVg6x8lIId1mnIzKAE1ksl_r054dsKK0NOqNDTUw2AMDP4CY601ViFqXQ7lRCtLGqz1H5JdEpjHM6alx6CPYxQJNXCR6QeoX0h1uP2IVBxnmsnOfEAME1pqBBkQ&sa=X&ved=2ahUKEwjDj5mJncyNAxUhqZUCHVsNDTAQtKgLegQIFhAB&biw=1163&bih=648&dpr=1.65#fpstate=ive&vld=cid:e34ea978,vid:kKzNL6PEmCg,st:0';
  }

  // Función para toggle de FAQ
  toggleFaq(faqNumber: number) {
    this.faqOpen[faqNumber] = !this.faqOpen[faqNumber];
  }
}
