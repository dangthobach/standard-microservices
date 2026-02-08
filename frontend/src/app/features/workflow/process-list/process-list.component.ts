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
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { WorkflowService } from '../../../core/services/workflow.service';
import { ProcessInstance } from '../../../core/models/workflow.model';

@Component({
    selector: 'app-process-list',
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
        MatMenuModule,
        MatSnackBarModule
    ],
    templateUrl: './process-list.component.html',
    styleUrls: ['./process-list.component.css']
})
export class ProcessListComponent implements OnInit {
    processes = signal<ProcessInstance[]>([]);
    loading = signal<boolean>(true);
    error = signal<string | null>(null);

    displayedColumns = ['id', 'processDefinitionKey', 'startTime', 'startUserId', 'status', 'actions'];

    // Computed signals
    totalProcesses = computed(() => this.processes().length);
    activeProcesses = computed(() => this.processes().filter(p => !p.endTime && !p.suspended).length);
    suspendedProcesses = computed(() => this.processes().filter(p => p.suspended).length);

    constructor(
        private workflowService: WorkflowService,
        private snackBar: MatSnackBar
    ) { }

    ngOnInit(): void {
        this.loadProcesses();
    }

    loadProcesses(): void {
        this.loading.set(true);
        this.error.set(null);

        this.workflowService.getProcessInstances().subscribe({
            next: (processes) => {
                this.processes.set(processes);
                this.loading.set(false);
            },
            error: (err) => {
                console.error('Error loading processes:', err);
                this.error.set('Failed to load process instances');
                this.loading.set(false);
            }
        });
    }

    getStatusLabel(process: ProcessInstance): string {
        if (process.endTime) return 'Completed';
        if (process.suspended) return 'Suspended';
        return 'Running';
    }

    getStatusClass(process: ProcessInstance): string {
        if (process.endTime) return 'status-completed';
        if (process.suspended) return 'status-suspended';
        return 'status-running';
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

    truncateId(id: string): string {
        return id.length > 12 ? `${id.substring(0, 12)}...` : id;
    }
}
