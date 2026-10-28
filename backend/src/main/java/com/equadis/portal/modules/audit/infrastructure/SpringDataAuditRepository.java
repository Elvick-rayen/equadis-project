package com.equadis.portal.modules.audit.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.equadis.portal.modules.audit.domain.AuditEntry;
import com.equadis.portal.modules.audit.domain.AuditRepository;
import com.equadis.portal.modules.audit.domain.AuditSubject;

/** Spring Data adapter for the audit log. */
interface SpringDataAuditRepository extends JpaRepository<AuditEntry, Long>, AuditRepository {

	@Override
	AuditEntry save(AuditEntry entry);

	@Query("select a from AuditEntry a where (:subjectType is null or a.subjectType = :subjectType)")
	Page<AuditEntry> search(@Param("subjectType") AuditSubject subjectType, Pageable pageable);

}
