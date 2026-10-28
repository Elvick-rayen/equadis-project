package com.equadis.portal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Checks the module structure with Spring Modulith: the business modules under
 * com.equadis.portal.modules plus the shared kernel are detected, and no module uses
 * another module's internal packages or depends on it in a cycle.
 */
class ModularityTest {

	private final ApplicationModules modules = ApplicationModules.of(PortalApplication.class);

	@Test
	void detectsTheSharedKernelAndEachBusinessModule() {
		assertThat(modules.stream().map(module -> module.getBasePackage().getName()))
			.containsExactlyInAnyOrder("com.equadis.portal.shared", "com.equadis.portal.modules.auth",
					"com.equadis.portal.modules.persona", "com.equadis.portal.modules.retailer",
					"com.equadis.portal.modules.supplier");
	}

	@Test
	void modulesOnlyUseEachOthersPublicApi() {
		modules.verify();
	}

}
