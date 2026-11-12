package fr.kevin.llps.payments.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.GeneralCodingRules.ACCESS_STANDARD_STREAMS;

@AnalyzeClasses(packages = "fr.kevin.llps.payments", importOptions = ImportOption.DoNotIncludeTests.class)
class CodingConventionsTest {

    private static final String[] NEW_PACKAGES = {
            "fr.kevin.llps.payments.domain..", "fr.kevin.llps.payments.adapter..", "fr.kevin.llps.payments.bootstrap.."};

    // AR-17 (domain part)
    @ArchTest
    static final ArchRule domainPublicFieldsShouldBeFinal =
            fields().that().areDeclaredInClassesThat().resideInAPackage("..domain..")
                    .and().arePublic()
                    .should().beFinal()
                    .because("java-coding-conventions.md Immutability / technical-layer-design.md AR-17: no public mutable fields in the domain");

    // AR-16 (REST)
    @ArchTest
    static final ArchRule restAdapterBeansShouldBePrefixedRest =
            classes().that().resideInAPackage("..adapter..")
                    .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .or().resideInAPackage("..adapter..")
                    .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
                    .should().haveSimpleNameStartingWith("Rest")
                    .because("java-coding-conventions.md Naming / AR-16: adapters are prefixed by their technology");

    // AR-16 (Kafka)
    @ArchTest
    static final ArchRule kafkaAdapterClassesShouldBePrefixedKafka =
            classes().that().resideInAPackage("..adapter.in.kafka..").and().areTopLevelClasses()
                    .should().haveSimpleNameStartingWith("Kafka")
                    .because("java-coding-conventions.md Naming / AR-16: adapters are prefixed by their technology");

    // AR-16 (JPA)
    @ArchTest
    static final ArchRule persistenceAdapterClassesShouldBePrefixedJpa =
            classes().that().resideInAPackage("..adapter.out.persistence..").and().areTopLevelClasses()
                    .should().haveSimpleNameStartingWith("Jpa")
                    .because("java-coding-conventions.md Naming / AR-16: adapters are prefixed by their technology");

    // AR-17 (field injection)
    @ArchTest
    static final ArchRule newCodeShouldNotUseFieldInjection =
            noFields().that().areDeclaredInClassesThat().resideInAnyPackage(NEW_PACKAGES)
                    .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .because("java-coding-conventions.md Dependency injection / AR-17: constructor injection only");

    // AR-17 (standard streams)
    @ArchTest
    static final ArchRule newCodeShouldNotUseStandardStreams =
            noClasses().that().resideInAnyPackage(NEW_PACKAGES)
                    .should(ACCESS_STANDARD_STREAMS)
                    .because("java-coding-conventions.md Logging / AR-17: no System.out or System.err");

    // AR-18 (methods)
    @ArchTest
    static final ArchRule transactionalMethodsShouldOnlyBeKafkaListeners =
            noMethods().that().areDeclaredInClassesThat().resideOutsideOfPackage("..adapter.in.kafka..")
                    .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .orShould().beAnnotatedWith("jakarta.transaction.Transactional")
                    .because("technical-layer-design.md TL-DR-10 / AR-18: transactions only start in the Kafka listeners");

    // AR-18 (classes)
    @ArchTest
    static final ArchRule noClassShouldBeTransactional =
            noClasses().that().resideInAPackage("fr.kevin.llps.payments..")
                    .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .orShould().beAnnotatedWith("jakarta.transaction.Transactional")
                    .because("technical-layer-design.md TL-DR-10 / AR-18: no class-level transaction boundary");
}
