export type AuditSubject = 'PARTNERSHIP' | 'OFFER';

export const AUDIT_SUBJECTS: readonly AuditSubject[] = ['PARTNERSHIP', 'OFFER'];

/** One recorded partnership or offer action. */
export interface AuditEntry {
  id: number;
  /** ISO instant. */
  occurredAt: string;
  /** "admin", or "admin (as SUPPLIER:3)" when an administrator acted as someone else. */
  actor: string;
  /** e.g. PARTNERSHIP_TERMINATED */
  action: string;
  subjectType: AuditSubject;
  subjectId: number;
  details: string | null;
}

export interface AuditSearch {
  page: number;
  size: number;
  subjectType: AuditSubject | null;
}
