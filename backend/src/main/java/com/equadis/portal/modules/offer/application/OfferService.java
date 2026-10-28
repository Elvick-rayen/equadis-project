package com.equadis.portal.modules.offer.application;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.offer.OfferClosed;
import com.equadis.portal.modules.offer.OfferCreated;
import com.equadis.portal.modules.offer.OfferEdited;
import com.equadis.portal.modules.offer.OfferPublished;
import com.equadis.portal.modules.offer.application.command.CloseOffer;
import com.equadis.portal.modules.offer.application.command.CreateOffer;
import com.equadis.portal.modules.offer.application.command.EditOffer;
import com.equadis.portal.modules.offer.application.command.PublishOffer;
import com.equadis.portal.modules.offer.domain.Offer;
import com.equadis.portal.modules.offer.domain.OfferRepository;
import com.equadis.portal.modules.supplier.SupplierQueries;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/**
 * A supplier's offer use cases. Every command names the supplier: an offer of another supplier
 * is reported as not found, so suppliers cannot even learn which offer ids exist.
 */
@Service
@Transactional
public class OfferService {

	private final OfferRepository offers;

	private final SupplierQueries suppliers;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	public OfferService(OfferRepository offers, SupplierQueries suppliers, ApplicationEventPublisher events, Clock clock) {
		this.offers = offers;
		this.suppliers = suppliers;
		this.events = events;
		this.clock = clock;
	}

	public Offer create(CreateOffer command) {
		if (!suppliers.exists(command.supplierId())) {
			throw new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND, "Supplier " + command.supplierId() + " not found");
		}
		requireUniqueReference(command.supplierId(), command.details().reference(), 0L);
		Offer saved = save(Offer.draft(command.supplierId(), command.details()));
		events.publishEvent(new OfferCreated(saved.id(), saved.supplierId(), saved.reference()));
		return saved;
	}

	public Offer edit(EditOffer command) {
		Offer offer = get(command.supplierId(), command.offerId());
		requireUniqueReference(command.supplierId(), command.details().reference(), offer.id());
		offer.edit(command.details());
		Offer saved = save(offer);
		events.publishEvent(new OfferEdited(saved.id(), saved.supplierId(), saved.reference()));
		return saved;
	}

	public Offer publish(PublishOffer command) {
		Offer offer = get(command.supplierId(), command.offerId());
		offer.publish(today());
		Offer saved = save(offer);
		events.publishEvent(new OfferPublished(saved.id(), saved.supplierId(), saved.reference()));
		return saved;
	}

	/** BR-08: listeners of OfferClosed run in this transaction and cancel the offer's subscriptions. */
	public Offer close(CloseOffer command) {
		Offer offer = get(command.supplierId(), command.offerId());
		offer.close();
		Offer saved = save(offer);
		events.publishEvent(new OfferClosed(saved.id(), saved.supplierId(), saved.reference(), today()));
		return saved;
	}

	@Transactional(readOnly = true)
	public Offer get(long supplierId, long offerId) {
		return offers.findById(offerId)
			.filter(offer -> offer.supplierId() == supplierId)
			.orElseThrow(() -> new BusinessException(ErrorCode.OFFER_NOT_FOUND, "Offer " + offerId + " not found"));
	}

	public LocalDate today() {
		return LocalDate.now(clock);
	}

	private void requireUniqueReference(long supplierId, String reference, long excludedId) {
		if (offers.existsReference(supplierId, reference, excludedId)) {
			throw referenceTaken(reference);
		}
	}

	private Offer save(Offer offer) {
		try {
			return offers.saveAndFlush(offer);
		}
		catch (DataIntegrityViolationException ex) {
			throw referenceTaken(offer.reference());
		}
	}

	private static BusinessException referenceTaken(String reference) {
		return new BusinessException(ErrorCode.OFFER_REFERENCE_TAKEN, "This supplier already has an offer '" + reference + "'");
	}

}
