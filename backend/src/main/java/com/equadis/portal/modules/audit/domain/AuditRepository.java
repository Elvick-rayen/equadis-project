package com.equadis.portal.modules.audit.domain;

/** Persistence port for the audit log. Entries are only ever added. */
public interface AuditRepository {

	AuditEntry save(AuditEntry entry);

}
