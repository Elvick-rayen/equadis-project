package com.equadis.portal.modules.retailer.web;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.modules.retailer.RetailerSummary;
import com.equadis.portal.modules.retailer.application.RetailerService;
import com.equadis.portal.modules.retailer.domain.Retailer;
import com.equadis.portal.modules.retailer.infrastructure.RetailerReadModel;
import com.equadis.portal.shared.actor.ActorAccess;
import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.shared.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/retailers")
@Tag(name = "Retailers")
class RetailerController {

	private final RetailerService service;

	private final RetailerReadModel readModel;

	private final ActorAccess access;

	RetailerController(RetailerService service, RetailerReadModel readModel, ActorAccess access) {
		this.service = service;
		this.readModel = readModel;
		this.access = access;
	}

	@GetMapping
	@Operation(summary = "Search retailers (administrator)",
			description = "Case-insensitive search on store name, e-mail, city and country. "
					+ "Sortable by storeName, storeType, contactEmail, location.city, location.countryCode.")
	PageResponse<RetailerResponse> search(CurrentActor actor,
			@Parameter(description = "Text to search for") @RequestParam(required = false) String q,
			@PageableDefault(size = 10, sort = "storeName", direction = Sort.Direction.ASC) Pageable pageable) {
		access.requireAdmin(actor);
		return PageResponse.of(readModel.search(q, pageable), RetailerWebMapper::toResponse);
	}

	@GetMapping("/summaries")
	@Operation(summary = "All retailers as id + name, for the administrator's \"Acting as\" selector")
	List<RetailerSummary> summaries(@AuthenticationPrincipal Jwt token) {
		access.requireAdministratorAccount(token);
		return readModel.findAllSummaries();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one retailer (administrator or the retailer itself)")
	RetailerResponse get(CurrentActor actor, @PathVariable long id) {
		access.requireAdminOrRetailer(actor, id);
		return RetailerWebMapper.toResponse(service.get(id));
	}

	@PostMapping
	@Operation(summary = "Create a retailer (administrator)")
	ResponseEntity<RetailerResponse> create(CurrentActor actor, @Valid @RequestBody RetailerRequest request) {
		access.requireAdmin(actor);
		Retailer created = service.create(RetailerWebMapper.toCreate(request));
		return ResponseEntity.created(URI.create("/api/retailers/" + created.id()))
			.body(RetailerWebMapper.toResponse(created));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a retailer (administrator)")
	RetailerResponse update(CurrentActor actor, @PathVariable long id, @Valid @RequestBody RetailerRequest request) {
		access.requireAdmin(actor);
		return RetailerWebMapper.toResponse(service.update(RetailerWebMapper.toUpdate(id, request)));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a retailer and its login account (administrator)")
	ResponseEntity<Void> delete(CurrentActor actor, @PathVariable long id) {
		access.requireAdmin(actor);
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

}
