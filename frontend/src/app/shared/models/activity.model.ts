import { UserSummary } from './user.model';

export type ActivityAction =
  | 'NOTE_CREATED'
  | 'NOTE_UPDATED'
  | 'NOTE_STATUS_CHANGED'
  | 'NOTE_MOVED'
  | 'NOTE_DELETED'
  | 'BOARD_CREATED'
  | 'BOARD_UPDATED'
  | 'BOARD_STATUS_CHANGED'
  | 'BOARD_DELETED';

export const ACTIVITY_ACTIONS: ActivityAction[] = [
  'NOTE_CREATED',
  'NOTE_UPDATED',
  'NOTE_STATUS_CHANGED',
  'NOTE_MOVED',
  'NOTE_DELETED',
  'BOARD_CREATED',
  'BOARD_UPDATED',
  'BOARD_STATUS_CHANGED',
  'BOARD_DELETED',
];

/** Nombre corto de cada acción (filtros y resúmenes). */
export const ACTIVITY_ACTION_LABELS: Record<ActivityAction, string> = {
  NOTE_CREATED: 'Creó nota',
  NOTE_UPDATED: 'Editó nota',
  NOTE_STATUS_CHANGED: 'Cambió estado de nota',
  NOTE_MOVED: 'Movió nota',
  NOTE_DELETED: 'Eliminó nota',
  BOARD_CREATED: 'Creó tablero',
  BOARD_UPDATED: 'Editó tablero',
  BOARD_STATUS_CHANGED: 'Cambió estado de tablero',
  BOARD_DELETED: 'Eliminó tablero',
};

export interface Activity {
  id: number;
  action: ActivityAction;
  user: UserSummary;
  boardId: number | null;
  boardName: string | null;
  noteId: number | null;
  noteTitle: string | null;
  noteOwner: UserSummary | null;
  fromValue: string | null;
  toValue: string | null;
  details: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
