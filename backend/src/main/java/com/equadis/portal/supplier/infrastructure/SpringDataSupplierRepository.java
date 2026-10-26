package com.equadis.portal.supplier.infrastructure;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.equadis.portal.supplier.SupplierSummary;
import com.equadis.portal.supplier.domain.Supplier;
import com.equadis.portal.supplier.domain.SupplierRepository;

/** Spring Data adapter: implements the domain port and adds read-side queries. */
interface SpringDataSupplierRepository extends JpaRepository<Supplier, Long>, SupplierRepository {

	@Query("""
			select s from Supplier s
			where lower(s.companyName) like :pattern
			   or lower(s.contact.name) like :pattern
			   or lower(s.contact.email) like :pattern
			   or lower(s.countryCode) like :pattern
			""")
	Page<Supplier> search(@Param("pattern") String pattern, Pageable pageable);

	@Query("select new com.equadis.portal.supplier.SupplierSummary(s.id, s.companyName) from Supplier s order by lower(s.companyName)")
	List<SupplierSummary> findAllSummaries();

	@Query("select new com.equadis.portal.supplier.SupplierSummary(s.id, s.companyName) from Supplier s where s.id in :ids order by lower(s.companyName)")
	List<SupplierSummary> findSummaries(@Param("ids") Collection<Long> ids);

}
