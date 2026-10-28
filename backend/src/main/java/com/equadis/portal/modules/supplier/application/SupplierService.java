package com.equadis.portal.modules.supplier.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.supplier.NewSupplier;
import com.equadis.portal.modules.supplier.SupplierRegistration;
import com.equadis.portal.modules.supplier.SupplierSummary;
import com.equadis.portal.modules.supplier.application.command.CreateSupplier;
import com.equadis.portal.modules.supplier.application.command.UpdateSupplier;
import com.equadis.portal.modules.supplier.domain.ContactInfo;
import com.equadis.portal.modules.supplier.domain.Supplier;
import com.equadis.portal.modules.supplier.domain.SupplierRepository;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.shared.domain.ErrorCode;

/** Supplier use cases. Enforces BR-01 (unique company name, case-insensitive). */
@Service
@Transactional
public class SupplierService implements SupplierRegistration {

	private final SupplierRepository suppliers;

	public SupplierService(SupplierRepository suppliers) {
		this.suppliers = suppliers;
	}

	@Override
	public SupplierSummary register(NewSupplier supplier) {
		Supplier created = create(new CreateSupplier(supplier.companyName(),
				new ContactInfo(supplier.contactName(), supplier.contactEmail(), supplier.contactPhone()),
				CountryCode.of(supplier.countryCode())));
		return new SupplierSummary(created.id(), created.companyName());
	}

	public Supplier create(CreateSupplier command) {
		String name = Supplier.normalizeName(command.companyName());
		if (suppliers.existsByCompanyNameIgnoreCase(name)) {
			throw nameTaken(name);
		}
		return saveUnique(Supplier.register(name, command.contact(), command.countryCode()), name);
	}

	public Supplier update(UpdateSupplier command) {
		Supplier supplier = get(command.supplierId());
		String name = Supplier.normalizeName(command.companyName());
		if (suppliers.existsByCompanyNameIgnoreCaseAndIdNot(name, supplier.id())) {
			throw nameTaken(name);
		}
		supplier.update(name, command.contact(), command.countryCode());
		return saveUnique(supplier, name);
	}

	/**
	 * Only possible while the supplier has no partnerships or offers: the database refuses
	 * otherwise (no cascading foreign keys), and that becomes SUPPLIER_IN_USE.
	 */
	public void delete(long supplierId) {
		Supplier supplier = get(supplierId);
		try {
			suppliers.delete(supplier);
			suppliers.flush();
		}
		catch (DataIntegrityViolationException ex) {
			throw new BusinessException(ErrorCode.SUPPLIER_IN_USE,
					"Supplier " + supplierId + " still has partnerships or offers and cannot be deleted");
		}
	}

	@Transactional(readOnly = true)
	public Supplier get(long supplierId) {
		return suppliers.findById(supplierId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND, "Supplier " + supplierId + " not found"));
	}

	/** The unique index is the last guard when two requests race on the same name. */
	private Supplier saveUnique(Supplier supplier, String name) {
		try {
			return suppliers.saveAndFlush(supplier);
		}
		catch (DataIntegrityViolationException ex) {
			throw nameTaken(name);
		}
	}

	private static BusinessException nameTaken(String name) {
		return new BusinessException(ErrorCode.SUPPLIER_NAME_TAKEN, "A supplier named '" + name + "' already exists");
	}

}
