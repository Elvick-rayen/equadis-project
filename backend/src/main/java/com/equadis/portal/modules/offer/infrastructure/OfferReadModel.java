package com.equadis.portal.modules.offer.infrastructure;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.offer.OfferAvailability;
import com.equadis.portal.modules.offer.OfferStatus;
import com.equadis.portal.modules.offer.OfferSummary;
import com.equadis.portal.modules.offer.domain.Offer;
import com.equadis.portal.modules.partnership.PartnershipQueries;
import com.equadis.portal.modules.supplier.SupplierQueries;
import com.equadis.portal.modules.supplier.SupplierSummary;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/**
 * Read side of the offer module: a supplier's offers, the offers available to a retailer, and
 * the public {@link OfferAvailability} where BR-10 is decided.
 */
@Component
@Transactional(readOnly = true)
public class OfferReadModel implements OfferAvailability {

	private static final Set<String> SORTABLE = Set.of("reference", "title", "validFrom", "validTo", "status");

	private final SpringDataOfferRepository repository;

	private final PartnershipQueries partnerships;

	private final SupplierQueries suppliers;

	private final Clock clock;

	OfferReadModel(SpringDataOfferRepository repository, PartnershipQueries partnerships, SupplierQueries suppliers,
			Clock clock) {
		this.repository = repository;
		this.partnerships = partnerships;
		this.suppliers = suppliers;
		this.clock = clock;
	}

	/** An offer available to a retailer, with its supplier's name. */
	public record AvailableOffer(Offer offer, String supplierName) {
	}

	public Page<Offer> forSupplier(long supplierId, OfferStatus status, Pageable pageable) {
		for (Sort.Order order : pageable.getSort()) {
			if (!SORTABLE.contains(order.getProperty())) {
				throw new BusinessException(ErrorCode.INVALID_SORT,
						"Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE);
			}
		}
		return repository.findForSupplier(supplierId, status, pageable);
	}

	/** BR-10: published, valid today, from a supplier with an ACTIVE partnership with this retailer. */
	public List<AvailableOffer> availableTo(long retailerId) {
		Set<Long> partnerSuppliers = partnerships.activeSupplierIds(retailerId);
		if (partnerSuppliers.isEmpty()) {
			return List.of();
		}
		Map<Long, String> names = suppliers.findSummaries(partnerSuppliers)
			.stream()
			.collect(Collectors.toMap(SupplierSummary::id, SupplierSummary::companyName));
		return repository.findOpenFromSuppliers(partnerSuppliers, today())
			.stream()
			.map(offer -> new AvailableOffer(offer, names.getOrDefault(offer.supplierId(), "")))
			.toList();
	}

	@Override
	public boolean isAvailableTo(long offerId, long retailerId) {
		return repository.findById(offerId)
			.filter(offer -> offer.isOpenOn(today()))
			.filter(offer -> partnerships.hasActivePartnership(offer.supplierId(), retailerId))
			.isPresent();
	}

	@Override
	public Optional<OfferSummary> findSummary(long offerId) {
		return repository.findById(offerId).map(OfferReadModel::summary);
	}

	@Override
	public List<OfferSummary> findSummaries(Collection<Long> offerIds) {
		return offerIds.isEmpty() ? List.of() : repository.findByIdIn(offerIds).stream().map(OfferReadModel::summary).toList();
	}

	public LocalDate today() {
		return LocalDate.now(clock);
	}

	private static OfferSummary summary(Offer offer) {
		return new OfferSummary(offer.id(), offer.supplierId(), offer.reference(), offer.title(), offer.discount().type(),
				offer.discount().value(),
				offer.discount().currency() == null ? null : offer.discount().currency().getCurrencyCode(),
				offer.validFrom(), offer.validTo(), offer.status());
	}

}
