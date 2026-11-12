package fr.kevin.llps.payments.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "fr.kevin.llps.payments", importOptions = ImportOption.DoNotIncludeTests.class)
class PortsTest {

    // AR-14 (input ports)
    @ArchTest
    static final ArchRule inputPortsShouldBeInterfaces =
            classes().that().resideInAPackage(DomainLayout.INPUT_PORTS).and().areTopLevelClasses()
                    .should().beInterfaces()
                    .because("isolated-domain-design.md §11 / AR-14: ..port.in.. only holds input-port interfaces");

    // AR-14 (command package, IT-02 gate layout)
    @ArchTest
    static final ArchRule commandPackagesShouldOnlyHoldCommands =
            classes().that().resideInAPackage(DomainLayout.COMMAND_PACKAGE).and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith(DomainLayout.COMMAND_SUFFIX)
                    .because("IT-02 gate layout / AR-14: the command package only holds *Command types");

    // AR-14 (query package, IT-02 gate layout)
    @ArchTest
    static final ArchRule queryPackagesShouldOnlyHoldQueriesAndCriteria =
            classes().that().resideInAPackage(DomainLayout.QUERY_PACKAGE).and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith(DomainLayout.QUERY_SUFFIX)
                    .orShould().haveSimpleNameEndingWith(DomainLayout.CRITERIA_SUFFIX)
                    .because("IT-02 gate layout / AR-14: the query package only holds *Query and *Criteria types");

    // AR-14 (repository ports)
    @ArchTest
    static final ArchRule repositoryPortsShouldBeInterfaces =
            classes().that().resideInAPackage(DomainLayout.OUTPUT_PORTS).and().areTopLevelClasses()
                    .should().beInterfaces()
                    .because("isolated-domain-design.md §11 / AR-14: output (repository) ports are interfaces in ..port.out..");

    // AR-14 (use-case naming, IT-02 gate layout): what implements an input port is a *Service in its context package
    @ArchTest
    static final ArchRule inputPortImplementationsShouldBeNamedService =
            classes().that().resideInAPackage(DomainLayout.DOMAIN)
                    .and().implement(resideInAPackage(DomainLayout.INPUT_PORTS))
                    .should().haveSimpleNameEndingWith(DomainLayout.SERVICE_SUFFIX)
                    .andShould().resideOutsideOfPackage(DomainLayout.PORTS)
                    .because("IT-02 gate layout / AR-14: use-case services are recognised by the *Service suffix");
}
