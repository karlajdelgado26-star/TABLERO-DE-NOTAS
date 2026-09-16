import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ROLE_LABELS } from '../../models/user.model';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './app-header.component.html',
  styleUrl: './app-header.component.css'
})
export class AppHeaderComponent {
  protected authService = inject(AuthService);
  protected readonly roleLabels = ROLE_LABELS;

  logout() {
    this.authService.logout();
  }
}
