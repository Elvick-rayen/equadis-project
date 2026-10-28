package com.equadis.portal.modules.offer.infrastructure;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.equadis.portal.modules.offer.OfferStatus;
import com.equadis.portal.modules.offer.domain.Offer;
import com.equadis.portal.modules.offer.domain.OfferRepository;

/** Spring Data adapter: implements the domain port and adds read-side queries. */
interface SpringDataOfferRepository extends JpaRepository<Offer, Long>, OfferRepository {

	/** Declared once here: both JpaRepository and the domain port have it. */
	@Override
	Optional<Offer> findById(Long id);

	@Override
	@Query("""
			select count(o) > 0 from Offer o
			where o.supplierId = :supplierId and lower(o.reference) = lower(:reference) and o.id <> :excludedId
			""")
	boolean existsReference(@Param("supplierId") long supplierId, @Param("reference") String reference,
			@Param("excludedId") long excludedId);

	@Query("select o from Offer o where o.supplierId = :supplierId and (:status is null or o.status = :status)")
	Page<Offer> findForSupplier(@Param("supplierId") long supplierId, @Param("status") OfferStatus status,
			Pageable pageable);

	/** BR-10, offer side: published and today within the validity window, for these suppliers. */
	@Query("""
			select o from Offer o
			where o.supplierId in :supplierIds
			  and o.status = com.equadis.portal.modules.offer.OfferStatus.PUBLISHED
			  and o.validFrom <= :today and o.validTo >= :today
			order by o.validTo asc, o.id asc
			""")
	List<Offer> findOpenFromSuppliers(@Param("supplierIds") Collection<Long> supplierIds, @Param("today") LocalDate today);

	List<Offer> findByIdIn(Collection<Long> ids);

}
