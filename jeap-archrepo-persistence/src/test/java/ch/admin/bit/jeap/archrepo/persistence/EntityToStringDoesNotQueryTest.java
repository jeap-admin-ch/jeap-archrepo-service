package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.metamodel.relation.CommandRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import ch.admin.bit.jeap.archrepo.metamodel.system.Frontend;
import ch.admin.bit.jeap.archrepo.metamodel.system.Gateway;
import ch.admin.bit.jeap.archrepo.metamodel.system.MobileApp;
import ch.admin.bit.jeap.archrepo.metamodel.system.SelfContainedSystem;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import ch.admin.bit.jeap.archrepo.metamodel.system.UnknownSystemComponent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>No entity's {@code toString()} may issue a database query.</b>
 * <p>
 * A {@code toString} is rendered from log statements and exception messages, in code that has no idea what it
 * costs. Over an eagerly rendered lazy association that means a query per log line - and, once the owner is
 * detached, a {@code LazyInitializationException} thrown from the logging framework, which is a defect in a
 * place nobody is looking. This service runs with {@code spring.jpa.open-in-view=false}, so a request that has
 * left its transaction has no session to fall back on.
 * <p>
 * Every entity of this model therefore declares {@code @ToString(onlyExplicitlyIncluded = true)} and lists the
 * plain columns worth printing, so that a field added later is silently safe rather than silently expensive.
 * This test is what keeps that true: it renders one instance of every entity and asserts that not a single
 * statement was prepared while doing so, and {@link #everyEntityThatDeclaresAToStringIsCovered()} fails if an
 * entity grows a {@code toString} without being added here.
 * <p>
 * It is a behavioural check on purpose. Lombok's choices are compiled away, so no amount of reflection over
 * annotations can tell which fields a generated {@code toString} reaches - and a hand-written one, like
 * {@link SystemComponentDatabaseSchema}'s, would escape such a check entirely.
 */
@DataJpaTest(properties = {
        "spring.flyway.locations=classpath:db/migration/common",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class EntityToStringDoesNotQueryTest {

    private static final String SYSTEM = "wvs";
    private static final byte[] BLOB = "{\"openapi\":\"3.0.0\"}".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private SystemRepository systemRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private SystemGraphRepository systemGraphRepository;
    @Autowired
    private ComponentGraphRepository componentGraphRepository;
    @Autowired
    private MessageGraphRepository messageGraphRepository;
    @Autowired
    private OpenApiSpecRepository openApiSpecRepository;
    @Autowired
    private SystemComponentDatabaseSchemaRepository databaseSchemaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    /** One persisted instance of every entity that declares a {@code toString}, by class and id. */
    private final Map<Class<?>, UUID> fixture = new LinkedHashMap<>();

    @BeforeEach
    void seedOneOfEverything() {
        statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();

        Team team = teamRepository.save(Team.builder().name("Team Blue").build());

        // One of each component type: they all carry @ToString(callSuper = true) over SystemComponent's
        SystemComponent backend = component(BackendService.builder().name("wvs-backend").ownedBy(team));
        SystemComponent frontend = component(Frontend.builder().name("wvs-frontend").ownedBy(team));
        SystemComponent gateway = component(Gateway.builder().name("wvs-gateway").ownedBy(team));
        SystemComponent mobile = component(MobileApp.builder().name("wvs-mobile").ownedBy(team));
        SystemComponent scs = component(SelfContainedSystem.builder().name("wvs-scs").ownedBy(team));
        SystemComponent unknown = component(UnknownSystemComponent.builder().name("wvs-unknown").ownedBy(team));

        System system = System.builder()
                .name(SYSTEM)
                .description("Warenverkehrssystem")
                .aliases(List.of("WVS-ALIAS"))
                .defaultOwner(team)
                .systemComponents(List.of(backend, frontend, gateway, mobile, scs, unknown))
                .build();

        RestApi restApi = RestApi.builder()
                .provider(backend).method("GET").path("/declarations/{id}").importer(Importer.OPEN_API)
                .build();
        system.addRestApi(restApi);

        // One relation of each kind - all three carry @ToString(callSuper = true) over AbstractRelation's
        RestApiRelation restApiRelation = RestApiRelation.builder()
                .definingSystem(system).consumerName("wvs-frontend").restApi(restApi)
                .pactUrl("https://pactbroker.example.com/pacts/declarations")
                .lastSeen(ZonedDateTime.now()).importer(Importer.PACT_BROKER)
                .build();
        EventRelation eventRelation = EventRelation.builder()
                .definingSystem(system).providerName("wvs-backend").consumerName("wvs-frontend")
                .eventName("WvsDeclarationAcceptedEvent").importer(Importer.MESSAGE_TYPE_REGISTRY)
                .build();
        CommandRelation commandRelation = CommandRelation.builder()
                .definingSystem(system).providerName("wvs-backend").consumerName("wvs-frontend")
                .commandName("WvsCheckNctsReferabilityCommand").importer(Importer.MESSAGE_TYPE_REGISTRY)
                .build();
        system.addRelation(restApiRelation);
        system.addRelation(eventRelation);
        system.addRelation(commandRelation);

        systemRepository.saveAndFlush(system);

        // Both of these hold a @ManyToOne to the component without a cascade, so they are saved through their
        // own repositories once the component exists - as the push endpoints do
        OpenApiSpec spec = openApiSpecRepository.saveAndFlush(OpenApiSpec.builder()
                .provider(backend).version("1.4.2").serverUrl("https://declarations.example.com").content(BLOB)
                .build());
        SystemComponentDatabaseSchema schema = databaseSchemaRepository.saveAndFlush(
                SystemComponentDatabaseSchema.builder()
                        .systemComponent(backend).schema(BLOB).schemaVersion("42")
                        .build());

        SystemGraph systemGraph = systemGraphRepository.save(
                SystemGraph.builder().systemName(SYSTEM).graphData(BLOB).fingerprint("sha256:sys").build());
        ComponentGraph componentGraph = componentGraphRepository.save(ComponentGraph.builder()
                .systemName(SYSTEM).componentName("wvs-backend").graphData(BLOB).fingerprint("sha256:comp")
                .build());
        MessageGraph messageGraph = messageGraphRepository.save(MessageGraph.builder()
                .messageTypeName("WvsDeclarationAcceptedEvent").variant("default").graphData(BLOB)
                .fingerprint("sha256:msg").build());

        fixture.put(System.class, system.getId());
        fixture.put(BackendService.class, backend.getId());
        fixture.put(Frontend.class, frontend.getId());
        fixture.put(Gateway.class, gateway.getId());
        fixture.put(MobileApp.class, mobile.getId());
        fixture.put(SelfContainedSystem.class, scs.getId());
        fixture.put(UnknownSystemComponent.class, unknown.getId());
        fixture.put(RestApi.class, restApi.getId());
        fixture.put(RestApiRelation.class, restApiRelation.getId());
        fixture.put(EventRelation.class, eventRelation.getId());
        fixture.put(CommandRelation.class, commandRelation.getId());
        fixture.put(OpenApiSpec.class, spec.getId());
        fixture.put(SystemComponentDatabaseSchema.class, schema.getId());
        fixture.put(SystemGraph.class, systemGraph.getId());
        fixture.put(ComponentGraph.class, componentGraph.getId());
        fixture.put(MessageGraph.class, messageGraph.getId());

        entityManager.flush();
        entityManager.clear();
    }

    /**
     * The rule itself. Each entity is loaded on its own into an empty persistence context - so its lazy
     * associations start uninitialised, exactly as they are in a request that loaded the row and nothing else -
     * and then rendered. The statements the load itself costs are spent before the count is taken.
     */
    @Test
    void noEntityToStringIssuesADatabaseQuery() {
        List<String> offenders = new ArrayList<>();

        fixture.forEach((type, id) -> {
            entityManager.clear();
            Object entity = entityManager.find(type, id);
            assertThat(entity).as("fixture for %s", type.getSimpleName()).isNotNull();

            long before = statistics.getPrepareStatementCount();
            String rendered = entity.toString();
            long statements = statistics.getPrepareStatementCount() - before;

            if (statements > 0) {
                offenders.add("%s.toString() issued %d statement(s) and rendered %s"
                        .formatted(type.getSimpleName(), statements, rendered));
            }
        });

        assertThat(offenders)
                .as("a toString is rendered from log statements and exception messages, so it must never reach "
                    + "the database - keep the entity on @ToString(onlyExplicitlyIncluded = true) and include "
                    + "only plain columns")
                .isEmpty();
    }

    /**
     * That the list above stays complete. An entity that grows a {@code toString} without being added to the
     * fixture would otherwise be unguarded, which is how {@code System} and the three graph entities came to
     * render their collections and blobs in the first place.
     * <p>
     * Abstract entities are covered by a concrete subclass: {@code AbstractRelation} and
     * {@code SystemComponent} declare the {@code toString} their subtypes call through {@code callSuper}.
     */
    @Test
    void everyEntityThatDeclaresAToStringIsCovered() {
        List<String> uncovered = entityManager.getMetamodel().getEntities().stream()
                .map(EntityType::getJavaType)
                .filter(EntityToStringDoesNotQueryTest::declaresToString)
                .filter(type -> fixture.keySet().stream().noneMatch(type::isAssignableFrom))
                .map(Class::getName)
                .sorted()
                .toList();

        assertThat(uncovered)
                .as("these entities declare a toString that nothing checks - add one instance of each to the "
                    + "fixture of this test")
                .isEmpty();
    }

    /**
     * A rendering that is safe but empty would pass the check above, so this pins what the entities that get
     * logged actually say. It is also the readable record of what "necessary and relevant" was decided to mean.
     */
    @Test
    void whatTheLoggedEntitiesRender() {
        entityManager.clear();

        assertThat(entityManager.find(System.class, fixture.get(System.class)).toString())
                .isEqualTo("System(id=%s, name=%s)".formatted(fixture.get(System.class), SYSTEM));

        assertThat(entityManager.find(RestApi.class, fixture.get(RestApi.class)).toString())
                .isEqualTo("RestApi(id=%s, method=GET, path=/declarations/{id})"
                        .formatted(fixture.get(RestApi.class)));

        assertThat(entityManager.find(EventRelation.class, fixture.get(EventRelation.class)).toString())
                .as("the relation importers stay out, and the base class contributes its own columns rather "
                    + "than an identity hash")
                .isEqualTo(("EventRelation(super=AbstractRelation(id=%s, consumerName=wvs-frontend, "
                            + "status=ACTIVE), eventName=WvsDeclarationAcceptedEvent)")
                        .formatted(fixture.get(EventRelation.class)));

        assertThat(entityManager.find(BackendService.class, fixture.get(BackendService.class)).toString())
                .isEqualTo("BackendService(super=SystemComponent(id=%s, name=wvs-backend))"
                        .formatted(fixture.get(BackendService.class)));

        assertThat(entityManager.find(SystemGraph.class, fixture.get(SystemGraph.class)).toString())
                .as("the graph blob must not be rendered")
                .isEqualTo("SystemGraph(id=%s, systemName=%s, fingerprint=sha256:sys)"
                        .formatted(fixture.get(SystemGraph.class), SYSTEM));

        assertThat(entityManager.find(SystemComponentDatabaseSchema.class,
                fixture.get(SystemComponentDatabaseSchema.class)).toString())
                .as("hand-written, and no longer naming the lazily loaded system")
                .isEqualTo(("SystemComponentDatabaseSchema{id=%s, systemComponent=wvs-backend, "
                            + "schemaVersion=42}")
                        .formatted(fixture.get(SystemComponentDatabaseSchema.class)));
    }

    private static boolean declaresToString(Class<?> type) {
        try {
            type.getDeclaredMethod("toString");
            return true;
        } catch (NoSuchMethodException notDeclaredHere) {
            return false;
        }
    }

    private static <T extends SystemComponent> T component(
            ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent.SystemComponentBuilder<?, ?> builder) {
        @SuppressWarnings("unchecked")
        T component = (T) builder.id(UUID.randomUUID()).createdAt(ZonedDateTime.now()).build();
        return component;
    }
}
