import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

// Angular Material imports
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';

import { EmployeeRoadmapResponse, RoadmapCloseRequest } from '../../models/Interfaces';

export interface CloseRoadmapDialogData {
  roadmap: EmployeeRoadmapResponse;
}

@Component({
  selector: 'app-close-roadmap-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatProgressBarModule,
    MatChipsModule
  ],
  templateUrl: './close-roadmap-modal.component.html',
  styleUrls: ['./close-roadmap-modal.component.css']
})
export class CloseRoadmapModalComponent {
  closeForm: FormGroup;
  roadmapProgress: { completed: number, total: number, percentage: number };

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<CloseRoadmapModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CloseRoadmapDialogData
  ) {
    // Calcular progreso
    this.roadmapProgress = this.calculateProgress();

    // Obtener hora actual
    const now = new Date();
    const currentTime = now.toTimeString().slice(0, 8); // HH:mm:ss

    // Crear formulario
    this.closeForm = this.fb.group({
      returnHour: [currentTime, [Validators.required, Validators.pattern(/^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$/)]],
      finalKm: ['', [Validators.required, Validators.min(0.1)]],
      observations: [''] // Campo opcional
    });

    // Si no hay precintos recolectados, hacer observaciones obligatorias
    if (this.roadmapProgress.completed === 0) {
      this.closeForm.get('observations')?.setValidators([Validators.required, Validators.minLength(10)]);
      this.closeForm.get('observations')?.updateValueAndValidity();
    }
  }

  private calculateProgress(): { completed: number, total: number, percentage: number } {
    const total = this.data.roadmap.generators.reduce((sum, gen) => sum + gen.totalBags, 0);
    const completed = this.data.roadmap.generators.reduce((sum, gen) => sum + gen.collectedBags, 0);
    const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;

    return { completed, total, percentage };
  }

  getProgressStatusClass(): string {
    if (this.roadmapProgress.percentage === 0) return 'status-pending';
    if (this.roadmapProgress.percentage === 100) return 'status-completed';
    return 'status-in-progress';
  }

  getProgressIcon(): string {
    if (this.roadmapProgress.percentage === 0) return 'schedule';
    if (this.roadmapProgress.percentage === 100) return 'check_circle';
    return 'sync';
  }

  isObservationsRequired(): boolean {
    return this.roadmapProgress.completed === 0;
  }

  onClose(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    if (this.closeForm.valid) {
      const formValue = this.closeForm.value;

      const request: RoadmapCloseRequest = {
        roadmapId: this.data.roadmap.roadmapId,
        returnHour: formValue.returnHour,
        finalKm: parseFloat(formValue.finalKm),
        observations: formValue.observations || undefined
      };

      this.dialogRef.close(request);
    } else {
      // Marcar todos los campos como touched para mostrar errores
      Object.keys(this.closeForm.controls).forEach(key => {
        this.closeForm.get(key)?.markAsTouched();
      });
    }
  }

  // Getter para facilitar acceso a los controles del formulario
  get f() {
    return this.closeForm.controls;
  }
}
