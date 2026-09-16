import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Note, NoteUpdateRequest } from '../../../../shared/models/note.model';

@Component({
  selector: 'app-note-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './note-card.component.html',
  styleUrls: ['./note-card.component.css']
})
export class NoteCardComponent {
  @Input() note!: Note;
  @Output() update = new EventEmitter<{ id: number, request: NoteUpdateRequest }>();
  @Output() delete = new EventEmitter<number>();

  isEditing = false;
  editTitle = '';
  editContent = '';
  editStatus: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' = 'PENDING';

  startEdit() {
    this.isEditing = true;
    this.editTitle = this.note.title;
    this.editContent = this.note.content;
    this.editStatus = this.note.status;
  }

  saveEdit() {
    this.update.emit({
      id: this.note.id,
      request: {
        title: this.editTitle,
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
