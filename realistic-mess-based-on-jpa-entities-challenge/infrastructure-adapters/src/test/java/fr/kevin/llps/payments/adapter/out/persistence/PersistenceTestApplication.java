package fr.kevin.llps.payments.adapter.out.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Test-only configuration for the persistence-adapter slices (TL-R4): the entities live outside this package.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "fr.kevin.llps.payments.entity")
public class PersistenceTestApplication {
}
