/**
 * Roles del sistema (cada uno incluye los permisos del siguiente):
 * - ADMIN:  administra usuarios + todo lo del LEADER
 * - LEADER: crea, modifica y elimina tableros; edita/elimina cualquier nota + todo lo del USER
 * - USER:   ve tableros, crea notas y edita/mueve/elimina solo las suyas
 */
export type Role = 'ADMIN' | 'LEADER' | 'USER';

export const ROLES: Role[] = ['ADMIN', 'LEADER', 'USER'];

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrador',
  LEADER: 'Líder',
  USER: 'Usuario',
};

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  active: boolean;
}

/** Autor de un tablero o una nota. */
export interface UserSummary {
  id: number;
  name: string;
  active: boolean;
}

export interface UserCreateRequest {
  name: string;
  email: string;
  password: string;
  role: Role;
}

export interface UserUpdateRequest {
  name: string;
  email: string;
  role: Role;
}
