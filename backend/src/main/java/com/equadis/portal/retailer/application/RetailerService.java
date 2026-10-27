package com.equadis.portal.retailer.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.retailer.application.command.CreateRetailer;
import com.equadis.portal.retailer.domain.Retailer;
import com.equadis.portal.retailer.domain.RetailerRepository;

/** Retailer use cases. For now only creation, used by sign-up. */
@Service
@Transactional
public class RetailerService {

	private final RetailerRepository retailers;

	public RetailerService(RetailerRepository retailers) {
		this.retailers = retailers;
	}

	public Retailer create(CreateRetailer command) {
		return retailers.saveAndFlush(
				Retailer.register(command.storeName(), command.storeType(), command.location(), command.contactEmail()));
	}

}
