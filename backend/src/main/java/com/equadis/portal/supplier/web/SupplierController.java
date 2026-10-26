package com.equadis.portal.supplier.web;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.shared.actor.ActorAccess;
import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.shared.web.PageResponse;
import com.equadis.portal.supplier.SupplierSummary;
import com.equadis.portal.supplier.application.SupplierService;
import com.equadis.portal.supplier.domain.Supplier;
import com.equadis.portal.supplier.infrastructure.SupplierReadModel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "Suppliers")
class SupplierController {

	private final SupplierService service;

	private final SupplierReadModel readModel;

	private final ActorAccess access;

	SupplierController(SupplierService service, SupplierReadModel readModel, ActorAccess access) {
		this.service = service;
		this.readModel = readModel;
		this.access = access;
	}

	@GetMapping
	@Operation(summary = "Search suppliers (administrator)",
			description = "Case-insensitive search on company name, contact and country. "
					+ "Sortable by companyName, contact.name, contact.email, countryCode.")
	PageResponse<SupplierResponse> search(CurrentActor actor,
			@Parameter(description = "Text to search for") @RequestParam(required = false) String q,
			@PageableDefault(size = 10, sort = "companyName", direction = Sort.Direction.ASC) Pageable pageable) {
		access.requireAdmin(actor);
		return PageResponse.of(readModel.search(q, pageable), SupplierWebMapper::toResponse);
	}

	@GetMapping("/summaries")
	@Operation(summary = "All suppliers as id + name, for the \"Acting as\" selector and pickers",
			description = "Open to every caller: the persona list is needed before a persona is chosen.")
	List<SupplierSummary> summaries() {
		return readModel.findAllSummaries();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one supplier (administrator or the supplier itself)")
	SupplierResponse get(CurrentActor actor, @PathVariable long id) {
		access.requireAdminOrSupplier(actor, id);
		return SupplierWebMapper.toResponse(service.get(id));
	}

	@PostMapping
	@Operation(summary = "Create a supplier (administrator)")
	ResponseEntity<SupplierResponse> create(CurrentActor actor, @Valid @RequestBody SupplierRequest request) {
		access.requireAdmin(actor);
		Supplier created = service.create(SupplierWebMapper.toCreate(request));
		return ResponseEntity.created(URI.create("/api/suppliers/" + created.id()))
			.body(SupplierWebMapper.toResponse(created));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a supplier (administrator)")
	SupplierResponse update(CurrentActor actor, @PathVariable long id, @Valid @RequestBody SupplierRequest request) {
		access.requireAdmin(actor);
		return SupplierWebMapper.toResponse(service.update(SupplierWebMapper.toUpdate(id, request)));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a supplier (administrator)")
	ResponseEntity<Void> delete(CurrentActor actor, @PathVariable long id) {
		access.requireAdmin(actor);
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

}
