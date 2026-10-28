/**
 * Shared kernel: error codes, value objects (CountryCode, Email), the current actor,
 * security, web and time configuration. Open module: every module may use any of its packages.
 */
@ApplicationModule(displayName = "Shared kernel", type = ApplicationModule.Type.OPEN)
package com.equadis.portal.shared;

import org.springframework.modulith.ApplicationModule;
