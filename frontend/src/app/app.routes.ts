import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { BoardListComponent } from './features/boards/board-list/board-list.component';
import { BoardComponent } from './features/board/board.component';
import { UsersComponent } from './features/admin/users/users.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, title: 'Iniciar sesión · Team Portal' },
  { path: 'boards', component: BoardListComponent, canActivate: [authGuard], title: 'Tableros · Team Portal' },
  { path: 'boards/:id', component: BoardComponent, canActivate: [authGuard], title: 'Tablero · Team Portal' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard],
    title: 'Dashboard · Team Portal'
  },
  {
    path: 'admin/users',
    component: UsersComponent,
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    title: 'Usuarios · Team Portal'
  },
  { path: 'board', redirectTo: '/boards', pathMatch: 'full' },
  { path: '', redirectTo: '/boards', pathMatch: 'full' },
  { path: '**', redirectTo: '/boards' }
];
