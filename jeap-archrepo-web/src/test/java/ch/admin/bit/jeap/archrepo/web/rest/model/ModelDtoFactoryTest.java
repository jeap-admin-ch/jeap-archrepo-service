package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.relation.CommandRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelDtoFactoryTest {

    @Test
    void createModelDto() {
        ModelDtoFactory modelDtoFactory = new ModelDtoFactory();
        ArchitectureModel model = ModelStub.createSimpleModel();

        ModelDto modelDto = modelDtoFactory.createModelDto(model);

        SystemDto systemDto = modelDto.getSystems().getFirst();
        assertEquals(ModelStub.SYSTEM, systemDto.getName());
        assertEquals("team", systemDto.getOwnedBy());
        SystemComponentDto systemComponentDto = systemDto.getSystemComponents().getFirst();
        assertEquals(ModelStub.COMPONENT, systemComponentDto.getName());
        assertEquals("desc", systemComponentDto.getDescription());
        assertEquals("Lagrev", systemComponentDto.getOwnedBy());
        assertEquals(SystemComponentType.BACKEND_SERVICE, systemComponentDto.getType());
    }

    @Test
    void createRestApiRelationDto() {
        ArchitectureModel model = ModelStub.createSimpleModelWithOneRestApiRelation();
        RestApiRelation restApiRelation = model.getRestApiRelationsWithoutPact().getFirst();
        Map<String, String> componentNameToSystemName = Map.of(
                "consumer", ModelStub.SYSTEM,
                "provider", ModelStub.SYSTEM);

        ModelDtoFactory modelDtoFactory = new ModelDtoFactory();
        RestApiRelationDto restApiRelationDto = modelDtoFactory.createRestApiRelationDto(componentNameToSystemName, restApiRelation);

        assertThat(restApiRelationDto.getProvider()).isEqualTo("provider");
        assertThat(restApiRelationDto.getProviderSystem()).isEqualTo(ModelStub.SYSTEM);
        assertThat(restApiRelationDto.getConsumer()).isEqualTo("consumer");
        assertThat(restApiRelationDto.getConsumerSystem()).isEqualTo(ModelStub.SYSTEM);
        assertThat(restApiRelationDto.getMethod()).isEqualTo("GET");
        assertThat(restApiRelationDto.getPath()).isEqualTo("/api/foo");
    }

    @Test
    void createRelationDto() {
        ArchitectureModel model = ModelStub.createSimpleModelWithOneRestApiRelation();
        EventRelation eventRelation = EventRelation.builder()
                .providerName("provider")
                .consumerName("consumer")
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .eventName("MyEvent")
                .build();
        CommandRelation commandRelation = CommandRelation.builder()
                .providerName("provider")
                .consumerName("consumer")
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .commandName("MyCommand")
                .build();
        System system = model.getSystems().getFirst();
        system.addRelation(eventRelation);
        system.addRelation(commandRelation);
        Map<String, String> componentNameToSystemName = Map.of(
                "consumer", ModelStub.SYSTEM,
                "provider", ModelStub.SYSTEM);

        RestApiRelation restApiRelation = (RestApiRelation) model.getAllActiveRelationsByType(REST_API_RELATION).getFirst();
        ModelDtoFactory modelDtoFactory = new ModelDtoFactory();
        RelationDto restApiRelationDto = modelDtoFactory.createRelationDto(componentNameToSystemName, restApiRelation);
        assertThat(restApiRelationDto.getProvider()).isEqualTo("provider");
        assertThat(restApiRelationDto.getProviderSystem()).isEqualTo(ModelStub.SYSTEM);
        assertThat(restApiRelationDto.getConsumer()).isEqualTo("consumer");
        assertThat(restApiRelationDto.getConsumerSystem()).isEqualTo(ModelStub.SYSTEM);
        assertThat(restApiRelationDto.getMethod()).isEqualTo("GET");
        assertThat(restApiRelationDto.getPath()).isEqualTo("/api/foo");

        RelationDto eventRelationDto = modelDtoFactory.createRelationDto(componentNameToSystemName, eventRelation);
        assertThat(eventRelationDto.getProvider()).isEqualTo("provider");
        assertThat(eventRelationDto.getProviderSystem()).isEqualTo(ModelStub.SYSTEM);
        assertThat(eventRelationDto.getConsumer()).isEqualTo("consumer");
        assertThat(eventRelationDto.getConsumerSystem()).isEqualTo(ModelStub.SYSTEM);
        assertThat(eventRelationDto.getMessageType()).isEqualTo("MyEvent");
        assertThat(eventRelationDto.getMethod()).isNull();
        assertThat(eventRelationDto.getPath()).isNull();

        RelationDto commandRelationDto = modelDtoFactory.createRelationDto(componentNameToSystemName, commandRelation);
        assertThat(commandRelationDto.getProvider()).isEqualTo("provider");
        assertThat(commandRelationDto.getProviderSystem()).isEqualTo(ModelStub.SYSTEM);
        assertThat(commandRelationDto.getConsumer()).isEqualTo("consumer");
        assertThat(commandRelationDto.getConsumerSystem()).isEqualTo(ModelStub.SYSTEM);
        assertThat(commandRelationDto.getMessageType()).isEqualTo("MyCommand");
        assertThat(commandRelationDto.getMethod()).isNull();
        assertThat(commandRelationDto.getPath()).isNull();
    }

    @Test
    void createRelationDtos() {
        ArchitectureModel model = ModelStub.createSimpleModelWithOneRestApiRelation();
        EventRelation eventRelation = EventRelation.builder()
                .providerName("provider")
                .consumerName("consumer")
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .eventName("MyEvent")
                .build();
        EventRelation eventDefinedBySystemButParticipantsFromOtherSystemRelation = EventRelation.builder()
                .providerName("othersystem-provider")
                .consumerName("othersystem-consumer")
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .eventName("AnotherEvent")
                .build();
        CommandRelation commandRelation = CommandRelation.builder()
                .providerName("provider")
                .consumerName("consumer")
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .commandName("MyCommand")
                .build();
        System system = model.getSystems().getFirst();
        system.addRelation(eventRelation);
        system.addRelation(eventDefinedBySystemButParticipantsFromOtherSystemRelation);
        system.addRelation(commandRelation);

        ModelDtoFactory modelDtoFactory = new ModelDtoFactory();
        List<RelationDto> relationDtos = modelDtoFactory.createRelationDtos(model, system);
        assertThat(relationDtos)
                .hasSize(3)
                .anyMatch(r -> r.getRelationType() == REST_API_RELATION && r.getPath().equals("/api/foo"))
                .anyMatch(r -> r.getRelationType() == EVENT_RELATION && r.getMessageType().equals("MyEvent"))
                .anyMatch(r -> r.getRelationType() == COMMAND_RELATION && r.getMessageType().equals("MyCommand"))
                .noneMatch(r -> r.getRelationType() == EVENT_RELATION && r.getMessageType().equals("AnotherEvent"));
    }
}
