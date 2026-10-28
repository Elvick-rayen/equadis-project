package com.equadis.portal.modules.partnership.application.command;

import com.equadis.portal.shared.domain.DateRange;

public record CreatePartnership(long supplierId, long retailerId, DateRange period) {

}
