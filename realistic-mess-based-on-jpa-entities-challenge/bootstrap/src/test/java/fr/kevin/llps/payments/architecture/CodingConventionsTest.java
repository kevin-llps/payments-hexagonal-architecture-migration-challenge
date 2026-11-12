package fr.kevin.llps.payments.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

@AnalyzeClasses(packages = "fr.kevin.llps.payments", importOptions = ImportOption.DoNotIncludeTests.class)
class CodingConventionsTest {

    // AR-17 (domain part)
    @ArchTest
    static final ArchRule domainPublicFieldsShouldBeFinal =
            fields().that().areDeclaredInClassesThat().resideInAPackage("..domain..")
                    .and().arePublic()
                    .should().beFinal()
                    .because("java-coding-conventions.md Immutability / technical-layer-design.md AR-17: no public mutable fields in the domain");
}
