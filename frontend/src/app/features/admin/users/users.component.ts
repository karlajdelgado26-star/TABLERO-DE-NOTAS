import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AppHeaderComponent } from '../../../shared/components/app-header/app-header.component';
import { ROLES, ROLE_LABELS, Role, User } from '../../../shared/models/user.model';
import { getErrorMessage } from '../../../shared/utils/http-error';
import { UserService } from '../services/user.service';

type FormMode = 'create' | 'edit' | 'password' | null;

@Component({
  selector: 'app-users',
  imports: [FormsModule, AppHeaderComponent],
  templateUrl: './users.component.html',
  styleUrl: './users.component.css'
})
export class UsersComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);

  protected readonly roles = ROLES;
  protected readonly roleLabels = ROLE_LABELS;

  protected users = signal<User[]>([]);
  protected loading = signal(true);
  protected saving = signal(false);
  protected errorMessage = signal('');
  protected successMessage = signal('');
  protected showInactive = signal(true);

  protected formMode = signal<FormMode>(null);
  protected selectedUser = signal<User | null>(null);

  protected visibleUsers = computed(() =>
    this.showInactive() ? this.users() : this.users().filter(user => user.active)
  );

  // Campos del formulario
  protected formName = '';
  protected formEmail = '';
  protected formRole: Role = 'USER';
  protected formPassword = '';
  protected formPasswordConfirm = '';

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading.set(true);
    this.userService.getUsers().subscribe({
      next: users => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: err => {
        this.errorMessage.set(getErrorMessage(err, 'Error al cargar los usuarios.'));
        this.loading.set(false);
      }
    });
  }

  isSelf(user: User): boolean {
    return this.authService.currentUser()?.id === user.id;
  }

  openCreate() {
    this.resetForm();
    this.selectedUser.set(null);
    this.formMode.set('create');
  }

  openEdit(user: User) {
    this.resetForm();
    this.formName = user.name;
    this.formEmail = user.email;
    this.formRole = user.role;
    this.selectedUser.set(user);
    this.formMode.set('edit');
  }

  openPassword(user: User) {
    this.resetForm();
    this.selectedUser.set(user);
    this.formMode.set('password');
  }

  closeForm() {
    this.formMode.set(null);
    this.selectedUser.set(null);
    this.resetForm();
  }

  submitForm() {
    this.errorMessage.set('');
    switch (this.formMode()) {
      case 'create':
        this.createUser();
        break;
      case 'edit':
        this.updateUser();
        break;
      case 'password':
        this.changePassword();
        break;
    }
  }

  deactivate(user: User) {
    if (!confirm(`¿Eliminar a ${user.name}? Quedará inactivo y no podrá iniciar sesión, pero sus notas se conservan.`)) {
      return;
    }
    this.setActive(user, false, `${user.name} fue desactivado.`);
  }

  activate(user: User) {
    this.setActive(user, true, `${user.name} fue reactivado.`);
  }

  private createUser() {
    if (!this.validateBasicFields() || !this.validatePassword()) {
      return;
    }
    this.run(
      this.userService.createUser({
        name: this.formName.trim(),
        email: this.formEmail.trim(),
        password: this.formPassword,
        role: this.formRole
      }),
      created => {
        this.users.update(users => this.sortByName([...users, created]));
        this.finish(`Usuario ${created.name} creado.`);
      },
      'Error al crear el usuario.'
    );
  }

  private updateUser() {
    const user = this.selectedUser();
    if (!user || !this.validateBasicFields()) {
      return;
    }
    this.run(
      this.userService.updateUser(user.id, {
        name: this.formName.trim(),
        email: this.formEmail.trim(),
        role: this.formRole
      }),
      updated => {
        this.replaceUser(updated);
        if (this.isSelf(updated)) {
          this.authService.refreshCurrentUser().subscribe();
        }
        this.finish(`Usuario ${updated.name} actualizado.`);
      },
      'Error al actualizar el usuario.'
    );
  }

  private changePassword() {
    const user = this.selectedUser();
    if (!user || !this.validatePassword()) {
      return;
    }
    this.run(
      this.userService.changePassword(user.id, this.formPassword),
      () => this.finish(`Contraseña de ${user.name} actualizada.`),
      'Error al cambiar la contraseña.'
    );
  }

  private setActive(user: User, active: boolean, successText: string) {
    this.errorMessage.set('');
    this.userService.setActive(user.id, active).subscribe({
      next: updated => {
        this.replaceUser(updated);
        this.successMessage.set(successText);
      },
      error: err => this.errorMessage.set(getErrorMessage(err, 'Error al cambiar el estado del usuario.'))
    });
  }

  private run<T>(request$: Observable<T>, onSuccess: (value: T) => void, fallbackError: string) {
    this.saving.set(true);
    this.successMessage.set('');
    request$.subscribe({
      next: value => {
        this.saving.set(false);
        onSuccess(value);
      },
      error: err => {
        this.saving.set(false);
        this.errorMessage.set(getErrorMessage(err, fallbackError));
      }
    });
  }

  private finish(successText: string) {
    this.successMessage.set(successText);
    this.closeForm();
  }

  private validateBasicFields(): boolean {
    if (!this.formName.trim() || !this.formEmail.trim()) {
      this.errorMessage.set('El nombre y el email son obligatorios.');
      return false;
    }
    return true;
  }

  private validatePassword(): boolean {
    if (this.formPassword.length < 8) {
      this.errorMessage.set('La contraseña debe tener al menos 8 caracteres.');
      return false;
    }
    if (this.formPassword !== this.formPasswordConfirm) {
      this.errorMessage.set('Las contraseñas no coinciden.');
      return false;
    }
    return true;
  }

  private replaceUser(updated: User) {
    this.users.update(users => this.sortByName(users.map(u => u.id === updated.id ? updated : u)));
  }

  private sortByName(users: User[]): User[] {
    return [...users].sort((a, b) => a.name.localeCompare(b.name));
  }

  private resetForm() {
    this.formName = '';
    this.formEmail = '';
    this.formRole = 'USER';
    this.formPassword = '';
    this.formPasswordConfirm = '';
  }
}
