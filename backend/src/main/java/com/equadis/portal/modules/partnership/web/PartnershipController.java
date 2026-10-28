package com.equadis.portal.modules.partnership.web;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.modules.partnership.PartnershipStatus;
import com.equadis.portal.modules.partnership.application.PartnershipService;
import com.equadis.portal.modules.partnership.application.command.RenewPartnership;
import com.equadis.portal.modules.partnership.application.command.TerminatePartnership;
import com.equadis.portal.modules.partnership.domain.Partnership;
import com.equadis.portal.modules.partnership.infrastructure.PartnershipReadModel;
import com.equadis.portal.shared.actor.ActorAccess;
import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.shared.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Partnerships are not edited or deleted: they are created, renewed (history kept) and
 * terminated, each through its own endpoint so each can check its own rule.
 */
@RestController
@RequestMapping("/api/partnerships")
@Tag(name = "Partnerships")
class PartnershipController {

	private final PartnershipService service;

	private final PartnershipReadModel readModel;

	private final ActorAccess access;

	PartnershipController(PartnershipService service, PartnershipReadModel readModel, ActorAccess access) {
		this.service = service;
		this.readModel = readModel;
		this.access = access;
	}

	@GetMapping
	@Operation(summary = "List partnerships (administrator)",
			description = "Filter by status (computed for today), or only ACTIVE ones ending within N days. "
					+ "Sortable by startDate, endDate.")
	PageResponse<PartnershipResponse> search(CurrentActor actor,
			@Parameter(description = "SCHEDULED, ACTIVE, EXPIRED or TERMINATED") @RequestParam(required = false) PartnershipStatus status,
			@Parameter(description = "Only ACTIVE partnerships ending within this many days") @RequestParam(required = false) Integer expiringWithinDays,
			@PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
		access.requireAdmin(actor);
		Page<Partnership> page = readModel.search(status, expiringWithinDays, pageable);
		PartnershipReadModel.PartyNames names = readModel.namesFor(page.getContent());
		return PageResponse.of(page, p -> PartnershipWebMapper.toSummary(p, names, readModel.today()));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a partnership with its renewal history (administrator or either company)")
	PartnershipResponse get(CurrentActor actor, @PathVariable long id) {
		Partnership partnership = service.get(id);
		access.requireAdminOrParty(actor, partnership.supplierId(), partnership.retailerId());
		return detail(partnership);
	}

	@PostMapping
	@Operation(summary = "Create a partnership (administrator) - BR-03, BR-04")
	ResponseEntity<PartnershipResponse> create(CurrentActor actor, @Valid @RequestBody PartnershipRequests.Create request) {
		access.requireAdmin(actor);
		Partnership created = service.create(PartnershipWebMapper.toCreate(request));
		return ResponseEntity.created(URI.create("/api/partnerships/" + created.id())).body(detail(created));
	}

	@PostMapping("/{id}/renewals")
	@Operation(summary = "Renew a partnership with a new end date (administrator) - BR-06")
	PartnershipResponse renew(CurrentActor actor, @PathVariable long id, @Valid @RequestBody PartnershipRequests.Renew request) {
		access.requireAdmin(actor);
		return detail(service.renew(new RenewPartnership(id, request.newEndDate())));
	}

	@PostMapping("/{id}/termination")
	@Operation(summary = "Terminate a partnership with a reason (administrator) - BR-07",
			description = "Also cancels the retailer's active subscriptions to the supplier's offers.")
	PartnershipResponse terminate(CurrentActor actor, @PathVariable long id,
			@Valid @RequestBody PartnershipRequests.Terminate request) {
		access.requireAdmin(actor);
		return detail(service.terminate(new TerminatePartnership(id, request.reason())));
	}

	private PartnershipResponse detail(Partnership partnership) {
		return PartnershipWebMapper.toDetail(partnership, readModel.namesFor(List.of(partnership)), readModel.today());
	}

}
