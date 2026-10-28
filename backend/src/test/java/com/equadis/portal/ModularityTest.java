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
			.contains("com.equadis.portal.shared", "com.equadis.portal.modules.supplier")
			.allSatisfy(name -> assertThat(name)
				.matches("com\\.equadis\\.portal\\.(shared|modules\\.[a-z]+)")
				.as("a module is the shared kernel or a direct child of com.equadis.portal.modules"));
	}

	@Test
	void modulesOnlyUseEachOthersPublicApi() {
		modules.verify();
	}

}
