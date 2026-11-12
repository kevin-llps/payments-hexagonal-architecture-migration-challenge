package fr.kevin.llps.payments.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "fr.kevin.llps.payments", importOptions = ImportOption.DoNotIncludeTests.class)
class CyclesTest {

    // AR-15
    @ArchTest
    static final ArchRule topLevelPackagesShouldBeFreeOfCycles =
            slices().matching("fr.kevin.llps.payments.(*)..")
                    .should().beFreeOfCycles()
                    .because("technical-layer-design.md §4.2 / §9.2 AR-15: the module dependency graph is acyclic");
}
