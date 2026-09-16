import { BoardStatus, BoardType } from './board.model';
import { NoteStatus } from './note.model';
import { Role, UserSummary } from './user.model';

export interface DashboardFilters {
  boardId: number | null;
  boardType: BoardType | null;
  boardStatus: BoardStatus | null;
  userId: number | null;
  /** yyyy-MM-dd */
  from: string | null;
  /** yyyy-MM-dd */
  to: string | null;
}

export const EMPTY_DASHBOARD_FILTERS: DashboardFilters = {
  boardId: null,
  boardType: null,
  boardStatus: null,
  userId: null,
  from: null,
  to: null,
};

export interface CountItem {
  key: string;
  count: number;
}

export interface StatusCounts {
  pending: number;
  inProgress: number;
  completed: number;
  total: number;
  completionRate: number;
}

export interface DashboardTotals {
  boards: number;
  notes: number;
  pending: number;
  inProgress: number;
  completed: number;
  completionRate: number;
  contributors: number;
  activities: number;
}

export interface BoardProgress {
  id: number;
  name: string;
  type: BoardType;
  status: BoardStatus;
  notes: StatusCounts;
  lastActivityAt: string | null;
}

export interface UserProgress {
  id: number;
  name: string;
  role: Role;
  active: boolean;
  notes: StatusCounts;
  activities: number;
  lastActivityAt: string | null;
}

export interface UserHighlight {
  id: number;
  name: string;
  notes: number;
}

export interface TimelinePoint {
  /** yyyy-MM-dd */
  date: string;
  created: number;
  completed: number;
}

export interface Timeline {
  from: string;
  to: string;
  granularity: 'DAY' | 'WEEK';
  points: TimelinePoint[];
}

export interface DashboardNoteRow {
  id: number;
  title: string;
  status: NoteStatus;
  boardId: number;
  boardName: string;
  boardType: BoardType;
  boardStatus: BoardStatus;
  createdBy: UserSummary | null;
  createdAt: string;
  updatedAt: string | null;
  completedAt: string | null;
}

export interface Dashboard {
  generatedAt: string;
  totals: DashboardTotals;
  notesByStatus: CountItem[];
  boardsByType: CountItem[];
  boardsByStatus: CountItem[];
  boards: BoardProgress[];
  users: UserProgress[];
  mostNotes: UserHighlight | null;
  fewestNotes: UserHighlight | null;
  activityByAction: CountItem[];
  timeline: Timeline;
  notes: DashboardNoteRow[];
}
