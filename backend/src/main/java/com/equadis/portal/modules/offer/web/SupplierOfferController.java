package com.equadis.portal.modules.offer.web;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.modules.offer.OfferStatus;
import com.equadis.portal.modules.offer.application.OfferService;
import com.equadis.portal.modules.offer.application.command.CloseOffer;
import com.equadis.portal.modules.offer.application.command.CreateOffer;
import com.equadis.portal.modules.offer.application.command.EditOffer;
import com.equadis.portal.modules.offer.application.command.PublishOffer;
import com.equadis.portal.modules.offer.domain.Offer;
import com.equadis.portal.modules.offer.infrastructure.OfferReadModel;
import com.equadis.portal.shared.actor.ActorAccess;
import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.shared.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** A supplier manages its own offers (an administrator may too). */
@RestController
@RequestMapping("/api/suppliers/{supplierId}/offers")
@Tag(name = "Offers")
class SupplierOfferController {

	private final OfferService service;

	private final OfferReadModel readModel;

	private final ActorAccess access;

	SupplierOfferController(OfferService service, OfferReadModel readModel, ActorAccess access) {
		this.service = service;
		this.readModel = readModel;
		this.access = access;
	}

	@GetMapping
	@Operation(summary = "The supplier's offers, optionally by status")
	PageResponse<OfferResponse> list(CurrentActor actor, @PathVariable long supplierId,
			@RequestParam(required = false) OfferStatus status,
			@PageableDefault(size = 10, sort = "validFrom", direction = Sort.Direction.DESC) Pageable pageable) {
		access.requireAdminOrSupplier(actor, supplierId);
		return PageResponse.of(readModel.forSupplier(supplierId, status, pageable),
				offer -> OfferWebMapper.toResponse(offer, readModel.today()));
	}

	@GetMapping("/{offerId}")
	@Operation(summary = "One of the supplier's offers")
	OfferResponse get(CurrentActor actor, @PathVariable long supplierId, @PathVariable long offerId) {
		access.requireAdminOrSupplier(actor, supplierId);
		return OfferWebMapper.toResponse(service.get(supplierId, offerId), service.today());
	}

	@PostMapping
	@Operation(summary = "Create a draft offer - BR-09")
	ResponseEntity<OfferResponse> create(CurrentActor actor, @PathVariable long supplierId,
			@Valid @RequestBody OfferRequest request) {
		access.requireAdminOrSupplier(actor, supplierId);
		Offer created = service.create(new CreateOffer(supplierId, OfferWebMapper.toDetails(request)));
		return ResponseEntity.created(URI.create("/api/suppliers/" + supplierId + "/offers/" + created.id()))
			.body(OfferWebMapper.toResponse(created, service.today()));
	}

	@PutMapping("/{offerId}")
	@Operation(summary = "Edit a draft offer - BR-08 (drafts only), BR-09")
	OfferResponse edit(CurrentActor actor, @PathVariable long supplierId, @PathVariable long offerId,
			@Valid @RequestBody OfferRequest request) {
		access.requireAdminOrSupplier(actor, supplierId);
		return OfferWebMapper.toResponse(service.edit(new EditOffer(supplierId, offerId, OfferWebMapper.toDetails(request))),
				service.today());
	}

	@PostMapping("/{offerId}/publish")
	@Operation(summary = "Publish a draft - BR-08")
	OfferResponse publish(CurrentActor actor, @PathVariable long supplierId, @PathVariable long offerId) {
		access.requireAdminOrSupplier(actor, supplierId);
		return OfferWebMapper.toResponse(service.publish(new PublishOffer(supplierId, offerId)), service.today());
	}

	@PostMapping("/{offerId}/close")
	@Operation(summary = "Close a published offer - BR-08", description = "Also cancels its subscriptions.")
	OfferResponse close(CurrentActor actor, @PathVariable long supplierId, @PathVariable long offerId) {
		access.requireAdminOrSupplier(actor, supplierId);
		return OfferWebMapper.toResponse(service.close(new CloseOffer(supplierId, offerId)), service.today());
	}

}
