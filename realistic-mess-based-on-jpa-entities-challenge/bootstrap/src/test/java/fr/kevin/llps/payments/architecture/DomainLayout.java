package fr.kevin.llps.payments.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;

/**
 * Class predicates of the per-context domain layout approved at the IT-02 gate:
 * {@code domain.<ctx>} holds the aggregate and the use-case services ({@code *Service}); {@code command} holds the
 * commands, {@code query} the search queries ({@code *Query}) and search criteria ({@code *Criteria}),
 * {@code port.in} the input ports and {@code port.out} the output ports. Commands and queries belong to the
 * domain-model ring and may be used by every adapter.
 */
final class DomainLayout {

    static final String DOMAIN = "fr.kevin.llps.payments.domain..";
    static final String INPUT_PORTS = "fr.kevin.llps.payments.domain.*.port.in..";
    static final String OUTPUT_PORTS = "fr.kevin.llps.payments.domain.*.port.out..";
    static final String PORTS = "fr.kevin.llps.payments.domain.*.port..";
    static final String COMMAND_PACKAGE = "fr.kevin.llps.payments.domain.*.command..";
    static final String QUERY_PACKAGE = "fr.kevin.llps.payments.domain.*.query..";
    static final String SERVICE_SUFFIX = "Service";
    static final String COMMAND_SUFFIX = "Command";
    static final String QUERY_SUFFIX = "Query";
    static final String CRITERIA_SUFFIX = "Criteria";

    /** Use-case services: {@code *Service} classes of the domain module, outside the port packages. */
    static final DescribedPredicate<JavaClass> USE_CASE_SERVICES =
            resideInAPackage(DOMAIN).and(not(resideInAPackage(PORTS))).and(simpleNameEndingWith(SERVICE_SUFFIX))
                    .as("use-case services (*Service in the domain module)");

    /** Application ring: use-case services and input ports. */
    static final DescribedPredicate<JavaClass> APPLICATION =
            USE_CASE_SERVICES.or(resideInAPackage(INPUT_PORTS)).as("use-case services and input ports");

    /**
     * Domain model: every domain class outside the port packages that is not a use-case service, i.e. aggregates,
     * the {@code command} and {@code query} packages (approved at the IT-02 gate) and the shared kernel.
     */
    static final DescribedPredicate<JavaClass> DOMAIN_MODEL =
            resideInAPackage(DOMAIN).and(not(resideInAPackage(PORTS))).and(not(USE_CASE_SERVICES))
                    .as("domain model (aggregates, command, query, shared kernel)");

    private DomainLayout() {
    }
}
