package com.equadis.portal.supplier.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;
import com.equadis.portal.supplier.application.command.CreateSupplier;
import com.equadis.portal.supplier.application.command.UpdateSupplier;
import com.equadis.portal.supplier.domain.Supplier;
import com.equadis.portal.supplier.domain.SupplierRepository;

/** Supplier use cases. Enforces BR-01 (unique company name, case-insensitive). */
@Service
@Transactional
public class SupplierService {

	private final SupplierRepository suppliers;

	public SupplierService(SupplierRepository suppliers) {
		this.suppliers = suppliers;
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

	public void delete(long supplierId) {
		suppliers.delete(get(supplierId));
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
