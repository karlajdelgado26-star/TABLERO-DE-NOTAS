export interface Note {
  id: number;
  title: string;
  content: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  positionX: number;
  positionY: number;
  createdAt: string;
}

export interface NoteCreateRequest {
  title: string;
  content: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  positionX: number;
  positionY: number;
}

export interface NoteUpdateRequest {
  title: string;
  content: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
}

export interface NotePositionRequest {
  positionX: number;
  positionY: number;
}
