import { UserSummary } from './user.model';

export type NoteStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';

export const NOTE_STATUS_LABELS: Record<NoteStatus, string> = {
  PENDING: 'Pendiente',
  IN_PROGRESS: 'En progreso',
  COMPLETED: 'Completada',
};

export interface Note {
  id: number;
  boardId: number;
  title: string;
  content: string | null;
  status: NoteStatus;
  positionX: number;
  positionY: number;
  createdBy: UserSummary | null;
  createdAt: string;
  updatedAt: string | null;
  completedAt: string | null;
  /** Lo calcula el backend según el rol y el autor de la nota. */
  canEdit: boolean;
  canDelete: boolean;
}

export interface NoteCreateRequest {
  title: string;
  content: string;
  status: NoteStatus;
  positionX: number;
  positionY: number;
}

export interface NoteUpdateRequest {
  title: string;
  content: string;
  status: NoteStatus;
}

export interface NotePositionRequest {
  positionX: number;
  positionY: number;
}
