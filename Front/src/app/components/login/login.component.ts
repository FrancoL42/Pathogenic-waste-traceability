import {Component, inject} from '@angular/core';
import {MatCard, MatCardActions, MatCardContent, MatCardHeader, MatCardTitle} from "@angular/material/card";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {MatError, MatFormField, MatLabel} from "@angular/material/form-field";
import {AuthService} from "../../services/auth.service";
import {Router} from "@angular/router";
import {CommonModule} from "@angular/common";
import {MatInput} from "@angular/material/input";
import {MatButton} from "@angular/material/button";
import {JwtHelperService} from "@auth0/angular-jwt";

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    MatCard,
    ReactiveFormsModule,
    MatFormField, MatLabel, MatError, MatCardTitle, MatCardContent, MatCardActions, CommonModule, MatInput, MatButton, MatCardHeader
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginForm!: FormGroup;
  jwtHelper = new JwtHelperService();

  // Estados del componente
  isLoading = false;
  showPassword = false;
  loginError = '';

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false] // Nuevo campo
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onLogin(): void {
    if (this.loginForm.invalid) {
      // Marcar todos los campos como touched para mostrar errores
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isLoading = true;
    this.loginError = '';

    const { email, password } = this.loginForm.value;

    this.authService.login(email, password).subscribe({
      next: (response: any) => {
        const token = response.token;
        localStorage.setItem('token', token);
        const decodedToken = this.jwtHelper.decodeToken(token);

        console.log('Decoded token:', decodedToken); // 🆕 Ver toda la estructura
        console.log('Token ID:', decodedToken.id);

        const roles = decodedToken.roles || [];
        const userId = decodedToken.id;
        localStorage.setItem('userId', userId);

        // Navegación basada en roles
        if (roles.includes('ROLE_ADMIN')) {
          this.router.navigate(['/admin']);
        } else if (roles.includes('ROLE_CLIENTE')) {
          localStorage.setItem('generatorId', decodedToken.id);
          this.router.navigate(['/dashboard']);
        } else if (roles.includes('ROLE_EMPLEADO')) {
          this.router.navigate(['/empleado']);
        } else if (roles.includes('ROLE_EMPLEADO_TRATADOR')) {
          this.router.navigate(['tratamiento'])
        }

        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Login error:', error);

        // Manejo de errores más específico
        if (error.status === 401) {
          this.loginError = 'Credenciales incorrectas. Verifica tu email y contraseña.';
        } else if (error.status === 0) {
          this.loginError = 'No se pudo conectar con el servidor. Verifica tu conexión.';
        } else {
          this.loginError = 'Error del servidor. Intenta nuevamente más tarde.';
        }
      },
    });
  }
}
