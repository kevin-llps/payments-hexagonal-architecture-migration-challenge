package fr.kevin.llps.payments.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@AnalyzeClasses(packages = "fr.kevin.llps.payments", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    // AR-05
    @ArchTest
    static final ArchRule useCasesAndPortsShouldNotDependOnFrameworksAdaptersOrEntities =
            noClasses().that(DomainLayout.APPLICATION.or(resideInAPackage(DomainLayout.OUTPUT_PORTS)))
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta..", "java.sql..", "com.fasterxml..", "org.apache.kafka..",
                            "org.hibernate..", "..adapter..", "fr.kevin.llps.payments.entity..")
                    .because("technical-layer-design.md TL-DR-3 / AR-05: use-case services and ports are framework-free and adapter-free");

    // AR-06
    @ArchTest
    static final ArchRule hexagonShouldFollowTheOnionArchitecture =
            onionArchitecture()
                    .domainModels(DomainLayout.DOMAIN_MODEL)
                    .domainServices(DomainLayout.OUTPUT_PORTS)
                    .applicationServices(DomainLayout.APPLICATION)
                    .adapter("rest", "..adapter.in.rest..")
                    .adapter("kafka", "..adapter.in.kafka..")
                    .adapter("persistence", "..adapter.out.persistence..")
                    // The composition root instantiates the use-case services with their ports; it is the outermost ring.
                    .adapter("composition root", "fr.kevin.llps.payments.bootstrap..")
                    .because("technical-layer-design.md §3 / AR-06: model <- output ports <- use cases and input ports <- adapters");
}
