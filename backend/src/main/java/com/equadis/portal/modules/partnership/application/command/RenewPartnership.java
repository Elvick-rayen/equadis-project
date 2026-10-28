package com.equadis.portal.modules.partnership.application.command;

import java.time.LocalDate;

public record RenewPartnership(long partnershipId, LocalDate newEndDate) {

}
