package com.equadis.portal.modules.subscription.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.equadis.portal.modules.subscription.domain.Subscription;
import com.equadis.portal.modules.subscription.domain.SubscriptionRepository;

/** Spring Data adapter: implements the domain port and adds read-side queries. */
interface SpringDataSubscriptionRepository extends JpaRepository<Subscription, Long>, SubscriptionRepository {

	@Override
	Optional<Subscription> findById(Long id);

	@Override
	@Query("select count(s) > 0 from Subscription s where s.retailerId = :retailerId and s.offerId = :offerId and s.cancelledOn is null")
	boolean existsActive(@Param("retailerId") long retailerId, @Param("offerId") long offerId);

	@Override
	@Query("select s from Subscription s where s.retailerId = :retailerId and s.supplierId = :supplierId and s.cancelledOn is null")
	List<Subscription> findActiveForRetailerAndSupplier(@Param("retailerId") long retailerId,
			@Param("supplierId") long supplierId);

	@Override
	@Query("select s from Subscription s where s.offerId = :offerId and s.cancelledOn is null")
	List<Subscription> findActiveForOffer(@Param("offerId") long offerId);

	/** Not cancelled first, newest first. */
	@Query("""
			select s from Subscription s where s.retailerId = :retailerId
			order by case when s.cancelledOn is null then 0 else 1 end, s.subscribedOn desc, s.id desc
			""")
	List<Subscription> findForRetailer(@Param("retailerId") long retailerId);

}
