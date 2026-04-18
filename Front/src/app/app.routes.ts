import { Routes } from '@angular/router';
import {AltaGeneradorComponent} from "./components/alta-generador/alta-generador.component";
import {VentaComponent} from "./components/venta/venta.component";
import {PagoExitosoComponent} from "./components/pago-exitoso/pago-exitoso.component";
import {PagoPendienteComponent} from "./components/pago-pendiente/pago-pendiente.component";
import {PagoFallidoComponent} from "./components/pago-fallido/pago-fallido.component";
import {HomeComponent} from "./components/home/home.component";
import {ClienteDashboardComponent} from "./components/cliente-dashboard/cliente-dashboard.component";
import {AdminComponent} from "./components/admin/admin.component";
import {LoginComponent} from "./components/login/login.component";
import {roleGuard} from "./guards/role.guard";
import {UnauthorizedComponent} from "./components/unauthorized/unauthorized.component";
import {EmpleadoComponent} from "./components/empleado/empleado.component";
import {EmpleadoWorkComponent} from "./components/empleado-work/empleado-work.component";
import {EmpleadoTratadorComponent} from "./components/empleado-tratador/empleado-tratador.component";
import {ZoneReportComponent} from "./components/zone-report/zone-report.component";
import {SalesReportComponent} from "./components/sales-report/sales-report.component";
import {TreatmentReportComponent} from "./components/treatment-report/treatment-report.component";

export const routes: Routes = [
  { path: 'alta', component: AltaGeneradorComponent },
  { path: 'venta', component: VentaComponent },
  { path: '', component: HomeComponent },
  { path: 'pago-exitoso', component: PagoExitosoComponent },
  { path: 'pago-pendiente', component: PagoPendienteComponent },
  { path: 'pago-fallido', component: PagoFallidoComponent },
  { path: 'unauthorized', component: UnauthorizedComponent },
  {path: 'empleado-work', component: EmpleadoWorkComponent },
  { path: 'prueba', component: EmpleadoTratadorComponent },
  { path: 'empleado',
    component: EmpleadoComponent ,
    canActivate: [roleGuard],
    data: { roles: ['ROLE_EMPLEADO', 'ROLE_ADMIN'] }
  },
  {
    path: 'empleado/work/:roadmapId',  // 🆕 Nueva ruta
    component: EmpleadoWorkComponent,
    canActivate: [roleGuard],
    data: { roles: ['ROLE_EMPLEADO'] }
  },

  {
    path: 'dashboard',
    component: ClienteDashboardComponent,
    canActivate: [roleGuard],
    data: { roles: ['ROLE_CLIENTE', 'ROLE_ADMIN']  }
  },
  {
    path: 'admin',
    component: AdminComponent,
    canActivate: [roleGuard],
    data: { roles: ['ROLE_ADMIN'] }
  },
  {
    path: 'tratamiento',
    component: EmpleadoTratadorComponent,
    canActivate: [roleGuard],
    data: { roles: ['ROLE_EMPLEADO_TRATADOR'] }
  },
  {
    path: 'reporte-zona',
    component: ZoneReportComponent
  },
  {
    path: 'reporte-venta',
    component: SalesReportComponent
  },
  { path: 'login', component: LoginComponent },
  {path: 'reporte-tratamiento', component: TreatmentReportComponent}
];

