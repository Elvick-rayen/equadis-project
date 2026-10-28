package com.equadis.portal.modules.audit.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.audit.domain.AuditEntry;
import com.equadis.portal.modules.audit.domain.AuditSubject;

/** The audit log, newest first. */
@Component
@Transactional(readOnly = true)
public class AuditReadModel {

	private final SpringDataAuditRepository repository;

	AuditReadModel(SpringDataAuditRepository repository) {
		this.repository = repository;
	}

	/** Always newest first; any sort requested by the client is ignored. */
	public Page<AuditEntry> search(AuditSubject subjectType, Pageable pageable) {
		Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
		return repository.search(subjectType, newestFirst);
	}

}
