package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.Relation;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How the {@code importers} element collections of relations and REST APIs are fetched.
 * <p>
 * Both are {@code FetchType.LAZY}, and that is not a detail: eager, an element collection on rows that arrive
 * through a collection is loaded one owning row at a time, and the reads of the architecture model traverse
 * hundreds of relations at once. This test holds the two halves of that in place - that nothing is loaded until
 * it is read, and that what is read comes back in a batch rather than in one query per row.
 * <p>
 * The batch size is set here explicitly because this module's tests do not load the web module's property
 * source. That the value the library actually ships in {@code archrepoDefaultProperties.properties} reaches
 * Hibernate is asserted against the real application context in {@code ImportersOverHttpIT}.
 */
@DataJpaTest(properties = {
        "spring.flyway.locations=classpath:db/migration/common",
        "spring.jpa.properties.hibernate.default_batch_fetch_size=100",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class ImportersFetchStrategyTest extends PostgresDataJpaTestBase {

    /**
     * Enough owning rows that one query per row is unmistakably different from a batch, and well inside a
     * batch size of 100 so that the batched case is a single query.
     */
    private static final int ROWS = 30;

    /**
     * What a batched load of {@value ROWS} collections may cost. One query suffices; two leaves room for the
     * batch being padded or split. Thirty - one per row - is the regression this guards against.
     */
    private static final int MAX_BATCHED_STATEMENTS = 2;

    private static final String SYSTEM = "wvs";
    private static final String PROVIDER = "wvs-provider-service";
    private static final String CONSUMER = "wvs-consumer-service";

    @Autowired
    private SystemRepository systemRepository;
    @Autowired
    private TeamRepository teamRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;
    private PersistenceUnitUtil persistenceUnitUtil;

    @BeforeEach
    void seedALandscapeWithManyRelations() {
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        Team team = teamRepository.save(Team.builder().name("Team Blue").build());
        SystemComponent provider = component(PROVIDER, team);
        SystemComponent consumer = component(CONSUMER, team);

        System system = System.builder()
                .name(SYSTEM)
                .defaultOwner(team)
                .systemComponents(List.of(provider, consumer))
                .build();

        for (int i = 0; i < ROWS; i++) {
            system.addRelation(EventRelation.builder()
                    .definingSystem(system)
                    .providerName(PROVIDER)
                    .consumerName(CONSUMER)
                    .eventName("WvsSomethingHappened" + i + "Event")
                    .importer(Importer.MESSAGE_TYPE_REGISTRY)
                    .build());

            RestApi restApi = RestApi.builder()
                    .provider(provider).method("GET").path("/declarations/" + i)
                    .importer(Importer.OPEN_API)
                    .build();
            system.addRestApi(restApi);
            system.addRelation(RestApiRelation.builder()
                    .definingSystem(system)
                    .consumerName(CONSUMER)
                    .restApi(restApi)
                    .lastSeen(ZonedDateTime.now())
                    .importer(Importer.GRAFANA)
                    .build());
        }

        systemRepository.saveAndFlush(system);

        // Read the aggregate back the way a request does, rather than seeing the instances just written
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void theImportersOfARelationAreNotLoadedUntilTheyAreRead() {
        Relation relation = anyEventRelation();

        assertThat(persistenceUnitUtil.isLoaded(relation, "importers"))
                .as("a relation must arrive without its importers - loading them per relation is what made "
                    + "reading the model cost one query per row")
                .isFalse();

        assertThat(relation.getImporters()).containsExactly(Importer.MESSAGE_TYPE_REGISTRY);

        assertThat(persistenceUnitUtil.isLoaded(relation, "importers"))
                .as("reading them must load them")
                .isTrue();
    }

    @Test
    void theImportersOfARestApiAreNotLoadedUntilTheyAreRead() {
        RestApi restApi = anyRestApi();

        assertThat(persistenceUnitUtil.isLoaded(restApi, "importers")).isFalse();
        assertThat(restApi.getImporters()).containsExactly(Importer.OPEN_API);
        assertThat(persistenceUnitUtil.isLoaded(restApi, "importers")).isTrue();
    }

    @Test
    void theImportersOfManyRelationsAreLoadedInOneBatch() {
        List<Relation> relations = loadedRelations();

        long statements = statementsSpentOn(() -> relations.forEach(relation -> relation.getImporters().size()));

        assertThat(statements)
                .as("the importers of %d relations must be batched, not fetched one relation at a time",
                        relations.size())
                .isLessThanOrEqualTo(MAX_BATCHED_STATEMENTS);
    }

    @Test
    void theImportersOfManyRestApisAreLoadedInOneBatch() {
        List<RestApi> restApis = loadedRestApis();

        long statements = statementsSpentOn(() -> restApis.forEach(restApi -> restApi.getImporters().size()));

        assertThat(statements)
                .as("the importers of %d REST APIs must be batched, not fetched one API at a time",
                        restApis.size())
                .isLessThanOrEqualTo(MAX_BATCHED_STATEMENTS);
    }

    /**
     * Lazy must not mean lossy: a relation seen by two sources still reports both, and the set stays sorted.
     */
    @Test
    void aLazilyLoadedImportersSetKeepsEveryValue() {
        Relation relation = anyEventRelation();
        relation.addImporter(Importer.GRAFANA);
        systemRepository.saveAndFlush(system());
        entityManager.flush();
        entityManager.clear();

        Relation reloaded = eventRelationNamed(relation.getLabel());

        assertThat(reloaded.getImporters())
                .containsExactly(Importer.GRAFANA, Importer.MESSAGE_TYPE_REGISTRY);
    }

    /**
     * Adding to a lazily loaded set has to initialise it first, or the row already stored would be lost. The
     * assertion above proves the value survives; this one proves the pre-existing value is still there, which
     * is the failure mode a naive lazy set would produce.
     */
    @Test
    void addingAnImporterToAStoredRelationDoesNotDropTheStoredOnes() {
        Relation relation = anyEventRelation();

        relation.addImporter(Importer.GRAFANA);

        assertThat(relation.getImporters())
                .as("the importer that was stored must still be there next to the one just added")
                .contains(Importer.MESSAGE_TYPE_REGISTRY, Importer.GRAFANA);
    }

    private long statementsSpentOn(Runnable work) {
        statistics.clear();
        long before = statistics.getPrepareStatementCount();
        work.run();
        return statistics.getPrepareStatementCount() - before;
    }

    /**
     * The relations of the system, with the list itself already initialised - so that the statements a test
     * measures are the ones spent on the {@code importers} sets and not on the relations.
     */
    private List<Relation> loadedRelations() {
        List<Relation> relations = system().getRelations();
        assertThat(relations).hasSize(2 * ROWS);
        return relations;
    }

    private List<RestApi> loadedRestApis() {
        List<RestApi> restApis = system().getRestApis();
        assertThat(restApis).hasSize(ROWS);
        return restApis;
    }

    private Relation anyEventRelation() {
        return system().getRelations().stream()
                .filter(EventRelation.class::isInstance)
                .findFirst().orElseThrow();
    }

    private Relation eventRelationNamed(String label) {
        return system().getRelations().stream()
                .filter(EventRelation.class::isInstance)
                .filter(relation -> label.equals(relation.getLabel()))
                .findFirst().orElseThrow();
    }

    private RestApi anyRestApi() {
        return system().getRestApis().getFirst();
    }

    private System system() {
        return systemRepository.findByNameIgnoreCase(SYSTEM).orElseThrow();
    }

    private static SystemComponent component(String name, Team team) {
        return BackendService.builder()
                .id(UUID.randomUUID())
                .name(name)
                .ownedBy(team)
                .createdAt(ZonedDateTime.now())
                .build();
    }
}
