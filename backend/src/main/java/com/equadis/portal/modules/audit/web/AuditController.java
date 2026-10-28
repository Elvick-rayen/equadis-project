package com.equadis.portal.modules.audit.web;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.modules.audit.domain.AuditSubject;
import com.equadis.portal.modules.audit.infrastructure.AuditReadModel;
import com.equadis.portal.shared.actor.ActorAccess;
import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.shared.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/audit-log")
@Tag(name = "Audit log")
class AuditController {

	record AuditEntryResponse(Long id, Instant occurredAt, String actor, String action, AuditSubject subjectType,
			long subjectId, String details) {
	}

	private final AuditReadModel readModel;

	private final ActorAccess access;

	AuditController(AuditReadModel readModel, ActorAccess access) {
		this.readModel = readModel;
		this.access = access;
	}

	@GetMapping
	@Operation(summary = "Partnership and offer actions, newest first (administrator)")
	PageResponse<AuditEntryResponse> list(CurrentActor actor, @RequestParam(required = false) AuditSubject subjectType,
			@PageableDefault(size = 20) Pageable pageable) {
		access.requireAdmin(actor);
		return PageResponse.of(readModel.search(subjectType, pageable), e -> new AuditEntryResponse(e.id(), e.occurredAt(),
				e.actor(), e.action(), e.subjectType(), e.subjectId(), e.details()));
	}

}
