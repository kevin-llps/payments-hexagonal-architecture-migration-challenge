package fr.kevin.llps.payments.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameStartingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "fr.kevin.llps.payments", importOptions = ImportOption.DoNotIncludeTests.class)
class AdapterDependenciesTest {

    // AR-08
    @ArchTest
    static final ArchRule drivingAdaptersShouldUseUseCasesOnlyThroughInputPorts =
            noClasses().that().resideInAPackage("..adapter.in..")
                    .should().dependOnClassesThat(DomainLayout.USE_CASE_SERVICES.or(resideInAPackage(DomainLayout.OUTPUT_PORTS)))
                    .because("technical-layer-design.md TL-DR-1 / AR-08: driving adapters call input ports (with commands and queries), never services or output ports");

    // AR-09
    @ArchTest
    static final ArchRule drivenAdaptersShouldNotDependOnUseCases =
            noClasses().that().resideInAPackage("..adapter.out..")
                    .should().dependOnClassesThat(DomainLayout.APPLICATION)
                    .because("technical-layer-design.md TL-DR-2 / AR-09: driven adapters never depend on use-case services or input ports");

    // AR-10
    @ArchTest
    static final ArchRule adaptersShouldNotDependOnEachOther =
            slices().matching("fr.kevin.llps.payments.adapter.(*).(*)..")
                    .should().notDependOnEachOther()
                    .because("technical-layer-design.md TL-DR-4 / AR-10: REST, Kafka and persistence adapters are independent");

    // AR-11 (entities)
    @ArchTest
    static final ArchRule entitiesShouldOnlyBeUsedByThePersistenceAdapter =
            noClasses().that().resideOutsideOfPackages("..adapter.out.persistence..", "fr.kevin.llps.payments.entity..")
                    .should().dependOnClassesThat().resideInAPackage("fr.kevin.llps.payments.entity..")
                    .because("technical-layer-design.md TL-DR-5 / AR-11: JPA entities stay behind the persistence adapter");

    // AR-11 (EntityManager)
    @ArchTest
    static final ArchRule entityManagerShouldOnlyBeUsedByThePersistenceAdapter =
            noClasses().that().resideOutsideOfPackage("..adapter.out.persistence..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("jakarta.persistence.EntityManager")
                    .because("technical-layer-design.md TL-DR-6 / AR-11: the EntityManager is only used by the persistence adapter");

    // AR-12
    @ArchTest
    static final ArchRule restAdapterShouldNotUseJdbcTypes =
            noClasses().that().resideInAPackage("..adapter.in.rest..")
                    .should().dependOnClassesThat().resideInAPackage("java.sql..")
                    .because("technical-layer-design.md TL-DR-9 / AR-12: no java.sql in the REST adapter");

    // AR-13 (load port)
    @ArchTest
    static final ArchRule jpaRepositoryAdaptersShouldImplementALoadPort =
            classes().that().resideInAPackage("..adapter.out.persistence..")
                    .and().haveSimpleNameEndingWith("RepositoryAdapter")
                    .should().implement(resideInAPackage(DomainLayout.OUTPUT_PORTS).and(simpleNameStartingWith("Load")))
                    .because("technical-layer-design.md §6 / AR-13: each JPA adapter implements its Load output port");

    // AR-13 (save port)
    @ArchTest
    static final ArchRule jpaRepositoryAdaptersShouldImplementASavePort =
            classes().that().resideInAPackage("..adapter.out.persistence..")
                    .and().haveSimpleNameEndingWith("RepositoryAdapter")
                    .should().implement(resideInAPackage(DomainLayout.OUTPUT_PORTS).and(simpleNameStartingWith("Save")))
                    .because("technical-layer-design.md §6 / AR-13: each JPA adapter implements its Save output port");
}
