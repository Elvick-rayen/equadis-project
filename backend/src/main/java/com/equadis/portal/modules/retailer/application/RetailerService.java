package com.equadis.portal.modules.retailer.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.retailer.NewRetailer;
import com.equadis.portal.modules.retailer.RetailerRegistration;
import com.equadis.portal.modules.retailer.RetailerSummary;
import com.equadis.portal.modules.retailer.application.command.CreateRetailer;
import com.equadis.portal.modules.retailer.application.command.UpdateRetailer;
import com.equadis.portal.modules.retailer.domain.Location;
import com.equadis.portal.modules.retailer.domain.Retailer;
import com.equadis.portal.modules.retailer.domain.RetailerRepository;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.shared.domain.Email;
import com.equadis.portal.shared.domain.ErrorCode;

/** Retailer use cases: create (also used by sign-up), read, update and delete. */
@Service
@Transactional
public class RetailerService implements RetailerRegistration {

	private final RetailerRepository retailers;

	public RetailerService(RetailerRepository retailers) {
		this.retailers = retailers;
	}

	@Override
	public RetailerSummary register(NewRetailer retailer) {
		// BR-02 is checked while building the location.
		Location location = Location.forStoreType(retailer.storeType(), CountryCode.of(retailer.countryCode()),
				retailer.city(), retailer.address(), retailer.website());
		Retailer created = create(
				new CreateRetailer(retailer.storeName(), retailer.storeType(), location, Email.of(retailer.contactEmail())));
		return new RetailerSummary(created.id(), created.storeName());
	}

	public Retailer create(CreateRetailer command) {
		return retailers.saveAndFlush(
				Retailer.register(command.storeName(), command.storeType(), command.location(), command.contactEmail()));
	}

	@Transactional(readOnly = true)
	public Retailer get(long retailerId) {
		return retailers.findById(retailerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RETAILER_NOT_FOUND, "Retailer " + retailerId + " not found"));
	}

	public Retailer update(UpdateRetailer command) {
		Retailer retailer = get(command.retailerId());
		retailer.update(command.storeName(), command.storeType(), command.location(), command.contactEmail());
		return retailers.saveAndFlush(retailer);
	}

	/** Also deletes the retailer's login account (app_user.retailer_id is ON DELETE CASCADE). */
	public void delete(long retailerId) {
		retailers.delete(get(retailerId));
	}

}
