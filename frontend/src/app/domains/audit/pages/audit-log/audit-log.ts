import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { translateObjectSignal, TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AUDIT_SUBJECTS, AuditSubject } from '../../models/audit.model';
import { AuditService } from '../../services/audit.service';

const PAGE_SIZE = 20;

type Labels = Record<string, string> | undefined;

/** Administrator → Audit log (bonus): partnership and offer actions, newest first. */
@Component({
  selector: 'app-audit-log',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoDirective, FormsModule, DatePipe, TableModule, TagModule, SelectModule, ButtonModule],
  templateUrl: './audit-log.html',
})
export class AuditLog {
  protected readonly auditService = inject(AuditService);
  private readonly _transloco = inject(TranslocoService);
  private readonly subjectLabels = translateObjectSignal('audit.subjects');

  protected readonly pageSize = PAGE_SIZE;
  protected readonly first = signal(0);
  protected readonly subjectType = signal<AuditSubject | null>(null);

  protected readonly subjectOptions = computed(() => {
    const labels = this.subjectLabels() as Labels;
    return [
      { label: this._transloco.translate('audit.allSubjects'), value: null },
      ...AUDIT_SUBJECTS.map((subject) => ({ label: labels?.[subject] ?? subject, value: subject as AuditSubject | null })),
    ];
  });

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    const size = event.rows ?? PAGE_SIZE;
    const first = event.first ?? 0;
    this.first.set(first);
    this.auditService.setSearch({ page: Math.floor(first / size), size, subjectType: this.subjectType() });
  }

  protected onSubjectChange(subjectType: AuditSubject | null): void {
    this.subjectType.set(subjectType);
    this.first.set(0);
    this.auditService.setSearch({ page: 0, size: this.auditService.search()?.size ?? PAGE_SIZE, subjectType });
  }
}
