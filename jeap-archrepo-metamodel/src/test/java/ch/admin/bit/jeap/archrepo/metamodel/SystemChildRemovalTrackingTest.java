package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Components and their artifacts are deleted explicitly, from what the model records as removed, instead of by
 * JPA orphan removal - which would also delete them when they only move to another system, depending on the order
 * the systems were loaded in. Every removal has to be recorded; a move must record nothing.
 */
class SystemChildRemovalTrackingTest {

    private System owner;
    private System other;
    private ArchitectureModel model;
    private SystemComponent component;
    private RestApi restApi;
    private OpenApiSpec spec;
    private SystemComponentDatabaseSchema schema;

    @BeforeEach
    void setUp() {
        owner = System.builder().name("owner").build();
        other = System.builder().name("other").build();
        component = SystemComponentFactory.createSystemComponent(owner, "owner-service", Importer.DEPLOYMENT_LOG);
        restApi = RestApi.builder().provider(component).method("GET").path("/things").importer(Importer.GRAFANA).build();
        owner.addRestApi(restApi);
        spec = OpenApiSpec.builder().provider(component).content(bytes()).build();
        owner.addOpenApiSpec(spec);
        schema = SystemComponentDatabaseSchema.builder().systemComponent(component).schema(bytes()).schemaVersion("1").build();
        owner.addDatabaseSchema(schema);
        model = ArchitectureModel.builder().systems(List.of(owner, other)).build();
    }

    @Test
    void removingAComponentRecordsItAndItsArtifacts() {
        model.remove(component);

        assertThat(owner.getSystemComponentsIncludingUndeployed()).isEmpty();
        assertThat(model.getRemovedSystemComponents()).containsExactly(component);
        assertThat(model.getRemovedArtifacts()).containsExactlyInAnyOrder(restApi, spec, schema);
    }

    @Test
    void removingByImporterRecordsTheComponentsAndRestApis() {
        model.removeAllByImporter(Importer.DEPLOYMENT_LOG);

        assertThat(model.getRemovedSystemComponents()).containsExactly(component);
        assertThat(model.getRemovedArtifacts()).contains(restApi);
    }

    @Test
    void cleanupRecordsTheUnusedRestApisItRemoves() {
        model.cleanup();

        assertThat(owner.getRestApis()).isEmpty();
        assertThat(model.getRemovedArtifacts()).containsExactly(restApi);
        assertThat(model.getRemovedSystemComponents()).isEmpty();
    }

    @Test
    void movingAComponentRecordsNothing() {
        SystemComponent misplaced = SystemComponentFactory.createSystemComponent(owner, "other-service", Importer.GRAFANA);

        model.findOrRelocateSystemComponent(other, misplaced.getName());

        assertThat(other.getSystemComponentsIncludingUndeployed()).containsExactly(misplaced);
        assertThat(model.getRemovedSystemComponents()).isEmpty();
        assertThat(model.getRemovedArtifacts()).isEmpty();
    }

    @Test
    void transferringAllComponentsRecordsNothing() {
        owner.transferSystemComponentsTo(other);

        assertThat(model.getRemovedSystemComponents()).isEmpty();
        assertThat(model.getRemovedArtifacts()).isEmpty();
    }

    @Test
    void aComponentPendingDeletionIsRevivedInsteadOfBeingCreatedAgain() {
        // One importer removes it (the deployment log no longer reports it), a later one in the same run still
        // sees it (metrics). Creating a new component of the same name would collide with the global unique index
        // on the name, because Hibernate inserts before it deletes; reviving it keeps one row.
        model.remove(component);

        var revived = model.findOrRelocateSystemComponent(owner, component.getName());

        assertThat(revived).containsSame(component);
        assertThat(owner.getSystemComponentsIncludingUndeployed()).containsExactly(component);
        assertThat(model.getRemovedSystemComponents()).isEmpty();
        assertThat(model.getRemovedArtifacts()).as("its artifacts stay removed")
                .containsExactlyInAnyOrder(restApi, spec, schema);
    }

    @Test
    void aComponentPendingDeletionIsRevivedIntoTheSystemItsNameBelongsTo() {
        // Reported for another system (RHOS: another namespace) than its name belongs to: it comes back where
        // it was, like a component that is not pending deletion stays where it is.
        model.remove(component);

        var revived = model.findOrRelocateSystemComponent(other, component.getName());

        assertThat(revived).containsSame(component);
        assertThat(owner.getSystemComponentsIncludingUndeployed()).containsExactly(component);
        assertThat(other.getSystemComponentsIncludingUndeployed()).isEmpty();
    }

    @Test
    void aComponentPendingDeletionIsRevivedIntoTheReportingSystemWhenItsNameBelongsThere() {
        SystemComponent misplaced = SystemComponentFactory.createSystemComponent(owner, "other-service", Importer.GRAFANA);
        model.remove(misplaced);

        var revived = model.findOrRelocateSystemComponent(other, misplaced.getName());

        assertThat(revived).containsSame(misplaced);
        assertThat(other.getSystemComponentsIncludingUndeployed()).containsExactly(misplaced);
    }

    @Test
    void removalsRecordedOnASystemThatIsThenRemovedAreStillReported() {
        model.remove(component);
        model.removeSystem(owner);

        assertThat(model.getRemovedSystemComponents()).containsExactly(component);
        assertThat(model.getRemovedArtifacts()).containsExactlyInAnyOrder(restApi, spec, schema);
    }

    private static byte[] bytes() {
        return "{}".getBytes(StandardCharsets.UTF_8);
    }
}
