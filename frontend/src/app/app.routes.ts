import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { BoardComponent } from './features/board/board.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'board', component: BoardComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/board', pathMatch: 'full' },
  { path: '**', redirectTo: '/board' }
];
