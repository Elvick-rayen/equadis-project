/**
 * Audit log (bonus): records who did which partnership and offer action, and when, by listening
 * to those modules' events after they commit. Read by administrators.
 */
@ApplicationModule(displayName = "Audit log")
package com.equadis.portal.modules.audit;

import org.springframework.modulith.ApplicationModule;
