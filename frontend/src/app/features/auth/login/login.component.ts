import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { getErrorMessage } from '../../../shared/utils/http-error';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  errorMessage = '';
  loading = false;

  ngOnInit() {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/boards']);
    }
  }

  onSubmit() {
    this.errorMessage = '';
    this.loading = true;
    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/boards']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = getErrorMessage(err, 'Credenciales inválidas o usuario inactivo.');
      }
    });
  }
}
