package fr.kevin.llps.payments.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "fr.kevin.llps.payments", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainIsolationTest {

    // AR-01
    @ArchTest
    static final ArchRule domainShouldNotDependOnFrameworksOrJdbc =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta..", "org.hibernate..", "com.fasterxml..",
                            "org.apache.kafka..", "java.sql..")
                    .because("isolated-domain-design.md DR-1 / technical-layer-design.md AR-01: the domain has no framework or JDBC dependency");

    // AR-01b
    @ArchTest
    static final ArchRule domainShouldOnlyDependOnCoreJdkAndDomain =
            classes().that().resideInAPackage("..domain..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage(
                            "java.lang..", "java.util..", "java.math..", "java.time..", "fr.kevin.llps.payments.domain..")
                    .because("isolated-domain-design.md DR-1: the domain imports only java.lang, java.util, java.math and java.time");

    // AR-02 (the shared kernel is shared by definition, DR-2; AR-03 keeps it independent of the contexts)
    @ArchTest
    static final ArchRule contextsShouldNotDependOnEachOther =
            slices().matching("fr.kevin.llps.payments.domain.(*)..")
                    .should().notDependOnEachOther()
                    .ignoreDependency(alwaysTrue(), resideInAPackage("fr.kevin.llps.payments.domain.sharedkernel.."))
                    .because("isolated-domain-design.md DR-2 / technical-layer-design.md AR-02 and AR-07: bounded contexts only share the shared kernel");

    // AR-03
    @ArchTest
    static final ArchRule sharedKernelShouldNotDependOnOtherDomainPackages =
            noClasses().that().resideInAPackage("..domain.sharedkernel..")
                    .should().dependOnClassesThat(resideInAPackage("..domain..")
                            .and(resideOutsideOfPackage("..domain.sharedkernel..")))
                    .because("isolated-domain-design.md DR-3: the shared kernel depends on no other domain package");

    // AR-04 (model part): name- and package-based since the IT-02 gate layout (model and *Service share a package)
    @ArchTest
    static final ArchRule domainModelShouldNotDependOnUseCasesOrPorts =
            noClasses().that(DomainLayout.DOMAIN_MODEL)
                    .should().dependOnClassesThat(DomainLayout.APPLICATION.or(resideInAPackage(DomainLayout.PORTS)))
                    .because("isolated-domain-design.md DR-6 / technical-layer-design.md AR-04: the domain model never depends on use-case services or ports");

    // AR-04 (outer layers part)
    @ArchTest
    static final ArchRule domainShouldNotDependOnAdaptersBootstrapOrEntities =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..adapter..", "..bootstrap..", "fr.kevin.llps.payments.entity..")
                    .because("isolated-domain-design.md DR-4 / technical-layer-design.md AR-04: the domain never depends on adapters, bootstrap or JPA entities");
}
