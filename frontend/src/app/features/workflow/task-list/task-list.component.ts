import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { WorkflowService } from '../../../core/services/workflow.service';
import { Task } from '../../../core/models/workflow.model';

@Component({
    selector: 'app-task-list',
    standalone: true,
    imports: [
        CommonModule,
        RouterModule,
        MatCardModule,
        MatIconModule,
        MatButtonModule,
        MatChipsModule,
        MatTableModule,
        MatTooltipModule,
        MatProgressSpinnerModule,
        MatBadgeModule,
        MatMenuModule,
        MatSnackBarModule
    ],
    templateUrl: './task-list.component.html',
    styleUrls: ['./task-list.component.css']
})
export class TaskListComponent implements OnInit {
    tasks = signal<Task[]>([]);
    loading = signal<boolean>(true);
    error = signal<string | null>(null);

    displayedColumns = ['name', 'processDefinitionId', 'assignee', 'priority', 'createTime', 'actions'];

    // Computed signals for task statistics
    totalTasks = computed(() => this.tasks().length);
    highPriorityTasks = computed(() => this.tasks().filter(t => t.priority >= 75).length);
    unassignedTasks = computed(() => this.tasks().filter(t => !t.assignee).length);

    constructor(
        private workflowService: WorkflowService,
        private snackBar: MatSnackBar
    ) { }

    ngOnInit(): void {
        this.loadTasks();
    }

    loadTasks(): void {
        this.loading.set(true);
        this.error.set(null);

        this.workflowService.getTasks().subscribe({
            next: (tasks) => {
                this.tasks.set(tasks);
                this.loading.set(false);
            },
            error: (err) => {
                console.error('Error loading tasks:', err);
                this.error.set('Failed to load tasks. Please try again.');
                this.loading.set(false);
            }
        });
    }

    claimTask(task: Task): void {
        // TODO: Get current user ID from auth service
        const currentUserId = 'current-user';

        this.workflowService.claimTask(task.id, currentUserId).subscribe({
            next: () => {
                this.snackBar.open(`Task "${task.name}" claimed successfully`, 'Close', { duration: 3000 });
                this.loadTasks();
            },
            error: (err) => {
                console.error('Error claiming task:', err);
                this.snackBar.open('Failed to claim task', 'Close', { duration: 3000 });
            }
        });
    }

    approveTask(task: Task): void {
        this.workflowService.completeTask(task.id, { approved: true, comment: 'Approved' }).subscribe({
            next: () => {
                this.snackBar.open(`Task "${task.name}" approved`, 'Close', { duration: 3000 });
                this.loadTasks();
            },
            error: (err) => {
                console.error('Error approving task:', err);
                this.snackBar.open('Failed to approve task', 'Close', { duration: 3000 });
            }
        });
    }

    rejectTask(task: Task): void {
        this.workflowService.completeTask(task.id, { approved: false, comment: 'Rejected' }).subscribe({
            next: () => {
                this.snackBar.open(`Task "${task.name}" rejected`, 'Close', { duration: 3000 });
                this.loadTasks();
            },
            error: (err) => {
                console.error('Error rejecting task:', err);
                this.snackBar.open('Failed to reject task', 'Close', { duration: 3000 });
            }
        });
    }

    getPriorityColor(priority: number): string {
        if (priority >= 75) return 'priority-high';
        if (priority >= 50) return 'priority-medium';
        return 'priority-low';
    }

    getPriorityLabel(priority: number): string {
        if (priority >= 75) return 'High';
        if (priority >= 50) return 'Medium';
        return 'Low';
    }

    formatDate(dateString: string): string {
        return new Date(dateString).toLocaleString();
    }

    getTimeAgo(dateString: string): string {
        const now = new Date();
        const date = new Date(dateString);
        const diff = now.getTime() - date.getTime();

        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);

        if (days > 0) return `${days}d ago`;
        if (hours > 0) return `${hours}h ago`;
        if (minutes > 0) return `${minutes}m ago`;
        return 'Just now';
    }
}
