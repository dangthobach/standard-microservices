import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';

import { WorkflowService } from '../../../core/services/workflow.service';
import { Task } from '../../../core/models/workflow.model';

/**
 * My Tasks Dashboard for Business Users
 * 
 * This component shows only the tasks assigned to the current user.
 * For advanced analytics and process monitoring, use Flowable Admin UI.
 */
@Component({
    selector: 'app-workflow-dashboard',
    standalone: true,
    imports: [
        CommonModule,
        RouterModule,
        MatCardModule,
        MatIconModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatChipsModule
    ],
    templateUrl: './workflow-dashboard.component.html',
    styleUrls: ['./workflow-dashboard.component.css']
})
export class WorkflowDashboardComponent implements OnInit {
    loading = signal<boolean>(true);
    myTasks = signal<Task[]>([]);
    error = signal<string | null>(null);

    // Computed stats for user's tasks
    pendingCount = computed(() => this.myTasks().length);
    highPriorityCount = computed(() => this.myTasks().filter(t => t.priority >= 75).length);

    constructor(private workflowService: WorkflowService) { }

    ngOnInit(): void {
        this.loadMyTasks();
    }

    loadMyTasks(): void {
        this.loading.set(true);
        this.error.set(null);

        // Load only tasks assigned to current user
        this.workflowService.getMyTasks().subscribe({
            next: (tasks) => {
                this.myTasks.set(tasks);
                this.loading.set(false);
            },
            error: (err) => {
                console.error('Error loading tasks:', err);
                this.error.set('Failed to load your tasks. Please try again.');
                this.loading.set(false);
            }
        });
    }

    refresh(): void {
        this.loadMyTasks();
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
