package com.equadis.portal.modules.subscription.web;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.modules.offer.DiscountType;
import com.equadis.portal.modules.offer.OfferSummary;
import com.equadis.portal.modules.subscription.application.SubscriptionService;
import com.equadis.portal.modules.subscription.domain.CancellationCause;
import com.equadis.portal.modules.subscription.domain.Subscription;
import com.equadis.portal.modules.subscription.domain.SubscriptionState;
import com.equadis.portal.modules.subscription.infrastructure.SubscriptionReadModel;
import com.equadis.portal.modules.subscription.infrastructure.SubscriptionReadModel.SubscriptionView;
import com.equadis.portal.shared.actor.ActorAccess;
import com.equadis.portal.shared.actor.CurrentActor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** A retailer's subscriptions (the retailer itself, or an administrator). */
@RestController
@RequestMapping("/api/retailers/{retailerId}/subscriptions")
@Tag(name = "Subscriptions")
class RetailerSubscriptionController {

	record SubscribeRequest(@NotNull Long offerId) {
	}

	record SubscriptionResponse(Long id, long offerId, String offerReference, String offerTitle, long supplierId,
			String supplierName, DiscountType discountType, BigDecimal discountValue, String currency,
			LocalDate validTo, LocalDate subscribedOn, LocalDate cancelledOn, CancellationCause cancellationCause,
			SubscriptionState state) {
	}

	private final SubscriptionService service;

	private final SubscriptionReadModel readModel;

	private final ActorAccess access;

	RetailerSubscriptionController(SubscriptionService service, SubscriptionReadModel readModel, ActorAccess access) {
		this.service = service;
		this.readModel = readModel;
		this.access = access;
	}

	@GetMapping
	@Operation(summary = "The retailer's subscriptions with their state: ACTIVE, SUSPENDED (BR-12) or CANCELLED")
	List<SubscriptionResponse> list(CurrentActor actor, @PathVariable long retailerId) {
		access.requireAdminOrRetailer(actor, retailerId);
		return readModel.forRetailer(retailerId).stream().map(RetailerSubscriptionController::toResponse).toList();
	}

	@PostMapping
	@Operation(summary = "Subscribe to an offer - BR-10, BR-11")
	ResponseEntity<SubscriptionResponse> subscribe(CurrentActor actor, @PathVariable long retailerId,
			@Valid @RequestBody SubscribeRequest request) {
		access.requireAdminOrRetailer(actor, retailerId);
		Subscription created = service.subscribe(retailerId, request.offerId());
		return ResponseEntity.created(URI.create("/api/retailers/" + retailerId + "/subscriptions/" + created.id()))
			.body(toResponse(readModel.view(created)));
	}

	@DeleteMapping("/{subscriptionId}")
	@Operation(summary = "Unsubscribe", description = "The subscription is kept as CANCELLED (cause RETAILER_REQUEST).")
	ResponseEntity<Void> unsubscribe(CurrentActor actor, @PathVariable long retailerId, @PathVariable long subscriptionId) {
		access.requireAdminOrRetailer(actor, retailerId);
		service.unsubscribe(retailerId, subscriptionId);
		return ResponseEntity.noContent().build();
	}

	private static SubscriptionResponse toResponse(SubscriptionView view) {
		Subscription s = view.subscription();
		OfferSummary offer = view.offer();
		return new SubscriptionResponse(s.id(), s.offerId(), offer == null ? null : offer.reference(),
				offer == null ? null : offer.title(), s.supplierId(), view.supplierName(),
				offer == null ? null : offer.discountType(), offer == null ? null : offer.discountValue(),
				offer == null ? null : offer.currency(), offer == null ? null : offer.validTo(), s.subscribedOn(),
				s.cancelledOn(), s.cancellationCause(), view.state());
	}

}
