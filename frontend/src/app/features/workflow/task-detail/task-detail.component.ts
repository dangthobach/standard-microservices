import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';

import { WorkflowService } from '../../../core/services/workflow.service';
import { Task, HistoricTask } from '../../../core/models/workflow.model';

@Component({
    selector: 'app-task-detail',
    standalone: true,
    imports: [
        CommonModule,
        RouterModule,
        ReactiveFormsModule,
        MatCardModule,
        MatIconModule,
        MatButtonModule,
        MatChipsModule,
        MatFormFieldModule,
        MatInputModule,
        MatProgressSpinnerModule,
        MatDividerModule,
        MatSnackBarModule,
        MatDialogModule
    ],
    templateUrl: './task-detail.component.html',
    styleUrls: ['./task-detail.component.css']
})
export class TaskDetailComponent implements OnInit {
    task = signal<Task | null>(null);
    taskVariables = signal<Record<string, any>>({});
    taskHistory = signal<HistoricTask[]>([]);
    loading = signal<boolean>(true);
    error = signal<string | null>(null);

    actionForm: FormGroup;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private workflowService: WorkflowService,
        private snackBar: MatSnackBar,
        private fb: FormBuilder
    ) {
        this.actionForm = this.fb.group({
            comment: ['', Validators.required]
        });
    }

    ngOnInit(): void {
        const taskId = this.route.snapshot.paramMap.get('id');
        if (taskId) {
            this.loadTask(taskId);
        } else {
            this.error.set('Task ID not provided');
            this.loading.set(false);
        }
    }

    loadTask(taskId: string): void {
        this.loading.set(true);

        // Load task details
        this.workflowService.getTask(taskId).subscribe({
            next: (task) => {
                this.task.set(task);
                this.loadTaskVariables(taskId);
                if (task.processInstanceId) {
                    this.loadTaskHistory(task.processInstanceId);
                }
                this.loading.set(false);
            },
            error: (err) => {
                console.error('Error loading task:', err);
                this.error.set('Failed to load task details');
                this.loading.set(false);
            }
        });
    }

    loadTaskVariables(taskId: string): void {
        this.workflowService.getTaskVariables(taskId).subscribe({
            next: (variables) => {
                this.taskVariables.set(variables);
            },
            error: (err) => console.error('Error loading variables:', err)
        });
    }

    loadTaskHistory(processInstanceId: string): void {
        this.workflowService.getTaskHistory(processInstanceId).subscribe({
            next: (history) => {
                this.taskHistory.set(history);
            },
            error: (err) => console.error('Error loading history:', err)
        });
    }

    approveTask(): void {
        const task = this.task();
        if (!task) return;

        const comment = this.actionForm.get('comment')?.value || 'Approved';

        this.workflowService.completeTask(task.id, {
            approved: true,
            comment
        }).subscribe({
            next: () => {
                this.snackBar.open('Task approved successfully', 'Close', { duration: 3000 });
                this.router.navigate(['/workflow/tasks']);
            },
            error: (err) => {
                console.error('Error approving task:', err);
                this.snackBar.open('Failed to approve task', 'Close', { duration: 3000 });
            }
        });
    }

    rejectTask(): void {
        const task = this.task();
        if (!task) return;

        const comment = this.actionForm.get('comment')?.value;
        if (!comment) {
            this.snackBar.open('Please provide a reason for rejection', 'Close', { duration: 3000 });
            return;
        }

        this.workflowService.completeTask(task.id, {
            approved: false,
            comment
        }).subscribe({
            next: () => {
                this.snackBar.open('Task rejected', 'Close', { duration: 3000 });
                this.router.navigate(['/workflow/tasks']);
            },
            error: (err) => {
                console.error('Error rejecting task:', err);
                this.snackBar.open('Failed to reject task', 'Close', { duration: 3000 });
            }
        });
    }

    claimTask(): void {
        const task = this.task();
        if (!task) return;

        const currentUserId = 'current-user'; // TODO: Get from auth service

        this.workflowService.claimTask(task.id, currentUserId).subscribe({
            next: () => {
                this.snackBar.open('Task claimed successfully', 'Close', { duration: 3000 });
                this.loadTask(task.id);
            },
            error: (err) => {
                console.error('Error claiming task:', err);
                this.snackBar.open('Failed to claim task', 'Close', { duration: 3000 });
            }
        });
    }

    goBack(): void {
        this.router.navigate(['/workflow/tasks']);
    }

    getPriorityColor(priority: number): string {
        if (priority >= 75) return 'priority-high';
        if (priority >= 50) return 'priority-medium';
        return 'priority-low';
    }

    formatDate(dateString: string): string {
        return new Date(dateString).toLocaleString();
    }

    getVariableKeys(): string[] {
        return Object.keys(this.taskVariables());
    }
}
