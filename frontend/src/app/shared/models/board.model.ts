import { UserSummary } from './user.model';

export type BoardType = 'PROJECT' | 'SPRINT' | 'SUPPORT' | 'MEETING' | 'OTHER';
export type BoardStatus = 'ACTIVE' | 'PAUSED' | 'FINISHED';

export const BOARD_TYPES: BoardType[] = ['PROJECT', 'SPRINT', 'SUPPORT', 'MEETING', 'OTHER'];
export const BOARD_STATUSES: BoardStatus[] = ['ACTIVE', 'PAUSED', 'FINISHED'];

export const BOARD_TYPE_LABELS: Record<BoardType, string> = {
  PROJECT: 'Proyecto',
  SPRINT: 'Sprint',
  SUPPORT: 'Soporte',
  MEETING: 'Reunión',
  OTHER: 'Otro',
};

export const BOARD_STATUS_LABELS: Record<BoardStatus, string> = {
  ACTIVE: 'Activo',
  PAUSED: 'En pausa',
  FINISHED: 'Finalizado',
};

export interface Board {
  id: number;
  name: string;
  description: string | null;
  type: BoardType;
  status: BoardStatus;
  createdBy: UserSummary | null;
  createdAt: string;
  updatedAt: string | null;
  noteCount: number;
}

export interface BoardRequest {
  name: string;
  description: string | null;
  type: BoardType;
  status: BoardStatus;
}
