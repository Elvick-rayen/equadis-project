package com.equadis.portal.modules.subscription.infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.offer.OfferAvailability;
import com.equadis.portal.modules.offer.OfferSummary;
import com.equadis.portal.modules.partnership.PartnershipQueries;
import com.equadis.portal.modules.subscription.domain.Subscription;
import com.equadis.portal.modules.subscription.domain.SubscriptionState;
import com.equadis.portal.modules.supplier.SupplierQueries;
import com.equadis.portal.modules.supplier.SupplierSummary;

/**
 * A retailer's subscriptions, with the offer, the supplier's name and the state shown to the
 * user (BR-12), all fetched through the other modules' public APIs.
 */
@Component
@Transactional(readOnly = true)
public class SubscriptionReadModel {

	private final SpringDataSubscriptionRepository repository;

	private final OfferAvailability offers;

	private final SupplierQueries suppliers;

	private final PartnershipQueries partnerships;

	SubscriptionReadModel(SpringDataSubscriptionRepository repository, OfferAvailability offers,
			SupplierQueries suppliers, PartnershipQueries partnerships) {
		this.repository = repository;
		this.offers = offers;
		this.suppliers = suppliers;
		this.partnerships = partnerships;
	}

	public record SubscriptionView(Subscription subscription, OfferSummary offer, String supplierName,
			SubscriptionState state) {
	}

	public List<SubscriptionView> forRetailer(long retailerId) {
		List<Subscription> subscriptions = repository.findForRetailer(retailerId);
		Map<Long, OfferSummary> offersById = offers
			.findSummaries(subscriptions.stream().map(Subscription::offerId).collect(Collectors.toSet()))
			.stream()
			.collect(Collectors.toMap(OfferSummary::id, Function.identity()));
		Set<Long> supplierIds = subscriptions.stream().map(Subscription::supplierId).collect(Collectors.toSet());
		Map<Long, String> supplierNames = suppliers.findSummaries(supplierIds)
			.stream()
			.collect(Collectors.toMap(SupplierSummary::id, SupplierSummary::companyName));
		// One partnership lookup per supplier, not per subscription.
		Map<Long, Boolean> activePartnership = new HashMap<>();
		supplierIds.forEach(id -> activePartnership.put(id, partnerships.hasActivePartnership(id, retailerId)));

		return subscriptions.stream()
			.map(s -> new SubscriptionView(s, offersById.get(s.offerId()), supplierNames.getOrDefault(s.supplierId(), ""),
					s.state(activePartnership.getOrDefault(s.supplierId(), false))))
			.toList();
	}

	public SubscriptionView view(Subscription subscription) {
		OfferSummary offer = offers.findSummary(subscription.offerId()).orElse(null);
		String supplierName = suppliers.findSummary(subscription.supplierId()).map(SupplierSummary::companyName).orElse("");
		boolean active = partnerships.hasActivePartnership(subscription.supplierId(), subscription.retailerId());
		return new SubscriptionView(subscription, offer, supplierName, subscription.state(active));
	}

}
