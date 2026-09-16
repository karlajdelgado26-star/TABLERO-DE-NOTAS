import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private authService = inject(AuthService);

  ngOnInit() {
    // Si hay sesión guardada, trae el rol y nombre actualizados desde el backend.
    // Si el usuario fue desactivado, el backend responde 401 y el interceptor cierra la sesión.
    if (this.authService.isAuthenticated()) {
      this.authService.refreshCurrentUser().subscribe({ error: () => undefined });
    }
  }
}
