/**
 * Partnerships between one supplier and one retailer: create, renew, terminate (BR-03 to BR-07).
 * Public API for other modules: {@link com.equadis.portal.modules.partnership.PartnershipQueries}
 * and the events {@link com.equadis.portal.modules.partnership.PartnershipCreated},
 * {@link com.equadis.portal.modules.partnership.PartnershipRenewed} and
 * {@link com.equadis.portal.modules.partnership.PartnershipTerminated}.
 */
@ApplicationModule(displayName = "Partnerships")
package com.equadis.portal.modules.partnership;

import org.springframework.modulith.ApplicationModule;
