import { Component, Input, Output, EventEmitter } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NOTE_STATUS_LABELS, Note, NoteStatus, NoteUpdateRequest } from '../../../../shared/models/note.model';

@Component({
  selector: 'app-note-card',
  imports: [NgClass, FormsModule],
  templateUrl: './note-card.component.html',
  styleUrls: ['./note-card.component.css']
})
export class NoteCardComponent {
  @Input({ required: true }) note!: Note;
  @Output() update = new EventEmitter<{ id: number, request: NoteUpdateRequest }>();
  @Output() delete = new EventEmitter<number>();

  protected readonly statusLabels = NOTE_STATUS_LABELS;

  isEditing = false;
  editTitle = '';
  editContent = '';
  editStatus: NoteStatus = 'PENDING';

  startEdit() {
    if (!this.note.canEdit) {
      return;
    }
    this.isEditing = true;
    this.editTitle = this.note.title;
    this.editContent = this.note.content ?? '';
    this.editStatus = this.note.status;
  }

  saveEdit() {
    const title = this.editTitle.trim();
    if (!title) {
      return;
    }
    this.update.emit({
      id: this.note.id,
      request: {
        title,
        content: this.editContent,
        status: this.editStatus
      }
    });
    this.isEditing = false;
  }

  cancelEdit() {
    this.isEditing = false;
  }

  onDelete() {
    if (confirm('¿Estás seguro de eliminar esta nota?')) {
      this.delete.emit(this.note.id);
    }
  }
}
