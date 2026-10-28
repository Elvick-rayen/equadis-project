package com.equadis.portal;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import jakarta.persistence.Entity;

/**
 * Backend architecture rules, the counterpart of the frontend's ESLint boundary rules.
 * Spring Modulith (ModularityTest) checks the module level: modules only use each other's
 * public API, with no cycles. These rules check the layers inside each module, and Spring
 * conventions. Each rule says why it exists; a failure lists every offending class.
 *
 * <pre>
 *   modules/&lt;name&gt;/
 *     web/            controllers, request/response records, mappers   (HTTP only)
 *     application/    services (use cases, transactions), commands
 *     domain/         entities, value objects, repository ports         (business rules)
 *     infrastructure/ Spring Data adapters, read models                 (database)
 *   shared/           shared kernel: may be used by every module, uses none of them
 * </pre>
 */
class ArchitectureTest {

	private static final String MODULES = "com.equadis.portal.modules..";

	private static final String SHARED = "com.equadis.portal.shared..";

	private static final JavaClasses CLASSES = new ClassFileImporter()
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
		.importPackages("com.equadis.portal");

	// --- Layers inside a module -------------------------------------------------------------

	@Test
	void layersOnlyDependDownwards() {
		check(layeredArchitecture().consideringOnlyDependenciesInLayers()
			.layer("Web").definedBy("..modules.*.web..")
			.layer("Application").definedBy("..modules.*.application..")
			.layer("Domain").definedBy("..modules.*.domain..")
			.layer("Infrastructure").definedBy("..modules.*.infrastructure..")
			.whereLayer("Web").mayNotBeAccessedByAnyLayer()
			.whereLayer("Application").mayOnlyBeAccessedByLayers("Web")
			.whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Web")
			.whereLayer("Domain").mayOnlyBeAccessedByLayers("Web", "Application", "Infrastructure")
			.because("requests flow web → application → domain; infrastructure implements the domain's ports"));
	}

	@Test
	void webNeverUsesRepositoriesDirectly() {
		check(noClasses().that().resideInAPackage("..modules.*.web..")
			.should().dependOnClassesThat(areRepositoriesOrNonReadModelInfrastructure())
			.because("controllers write through services and read through *ReadModel classes, never through repositories"));
	}

	@Test
	void domainIsFreeOfSpringAndOtherLayers() {
		check(noClasses().that().resideInAPackage("..modules.*.domain..")
			.should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "..web..", "..application..",
					"..infrastructure..")
			.because("business rules must not depend on the framework, HTTP or the database adapters"));
	}

	@Test
	void applicationDoesNotKnowAboutHttp() {
		check(noClasses().that().resideInAPackage("..modules.*.application..")
			.should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "org.springframework.http..",
					"jakarta.servlet..")
			.because("use cases are called by controllers but must stay usable without HTTP"));
	}

	@Test
	void sharedKernelDoesNotDependOnModules() {
		check(noClasses().that().resideInAPackage(SHARED)
			.should().dependOnClassesThat().resideInAPackage(MODULES)
			.because("every module may use shared, so shared must not use any module"));
	}

	// --- Where each kind of class lives ---------------------------------------------------------

	@Test
	void controllersLiveInWebAndAreNamedSo() {
		check(classes().that().areAnnotatedWith(RestController.class)
			.should().resideInAPackage("..modules.*.web..")
			.andShould().haveSimpleNameEndingWith("Controller")
			.because("HTTP endpoints belong to a module's web layer"));
	}

	@Test
	void servicesLiveInApplicationAndAreNamedSo() {
		check(classes().that().areAnnotatedWith(Service.class)
			.should().resideInAPackage("..modules.*.application..")
			.andShould().haveSimpleNameEndingWith("Service")
			.because("use cases belong to a module's application layer"));
	}

	@Test
	void entitiesLiveInTheDomain() {
		check(classes().that().areAnnotatedWith(Entity.class)
			.should().resideInAPackage("..modules.*.domain..")
			.because("entities are domain objects and carry the business rules"));
	}

	@Test
	void springDataRepositoriesLiveInInfrastructureAndStayPackagePrivate() {
		check(classes().that().areInterfaces().and().areAssignableTo(Repository.class)
			.should().resideInAPackage("..modules.*.infrastructure..")
			.andShould().notBePublic()
			.because("the domain declares ports; only infrastructure may know Spring Data"));
	}

	@Test
	void commandsAreImmutableRecords() {
		check(classes().that().resideInAPackage("..modules.*.application.command..")
			.should(beRecords())
			.because("a command is an immutable description of one intent"));
	}

	@Test
	void controllersNeverReturnEntities() {
		check(methods().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
			.should(notReturnEntities())
			.because("the API returns response records, so the database model can change without breaking clients"));
	}

	@Test
	void exceptionHandlingIsCentralized() {
		check(classes().that().areAnnotatedWith(RestControllerAdvice.class).or().areAnnotatedWith(ControllerAdvice.class)
			.should().resideInAPackage("com.equadis.portal.shared.web..")
			.because("one GlobalExceptionHandler turns every error into the same ProblemDetail shape"));
	}

	// --- General Java and Spring hygiene --------------------------------------------------------

	@Test
	void constructorInjectionOnly() {
		check(NO_CLASSES_SHOULD_USE_FIELD_INJECTION
			.because("constructor injection makes dependencies explicit, final and easy to test"));
	}

	@Test
	void noSystemOutOrJavaUtilLogging() {
		check(NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.because("use SLF4J loggers"));
		check(NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.because("use SLF4J loggers"));
	}

	@Test
	void noGenericExceptionsThrown() {
		check(NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
			.because("throw BusinessException with an ErrorCode, or a specific exception"));
	}

	// --- Helpers ------------------------------------------------------------------------------------

	private static void check(ArchRule rule) {
		rule.check(CLASSES);
	}

	private static DescribedPredicate<JavaClass> areRepositoriesOrNonReadModelInfrastructure() {
		return DescribedPredicate.describe("are repositories, or infrastructure classes other than *ReadModel",
				type -> type.getSimpleName().endsWith("Repository")
						|| type.getPackageName().contains(".infrastructure") && !isReadModel(type));
	}

	/** A *ReadModel class, or a result record nested inside one (e.g. PartnershipReadModel.PartyNames). */
	private static boolean isReadModel(JavaClass type) {
		return type.getSimpleName().endsWith("ReadModel")
				|| type.getEnclosingClass().map(outer -> outer.getSimpleName().endsWith("ReadModel")).orElse(false);
	}

	private static ArchCondition<JavaClass> beRecords() {
		return new ArchCondition<>("be records") {
			@Override
			public void check(JavaClass type, ConditionEvents events) {
				if (!type.isRecord()) {
					events.add(SimpleConditionEvent.violated(type, type.getName() + " is not a record"));
				}
			}
		};
	}

	private static ArchCondition<com.tngtech.archunit.core.domain.JavaMethod> notReturnEntities() {
		return new ArchCondition<>("not return @Entity types") {
			@Override
			public void check(com.tngtech.archunit.core.domain.JavaMethod method, ConditionEvents events) {
				JavaClass returned = method.getRawReturnType();
				if (returned.isAnnotatedWith(Entity.class)) {
					events.add(SimpleConditionEvent.violated(method,
							method.getFullName() + " returns the entity " + returned.getSimpleName()));
				}
			}
		};
	}

}
