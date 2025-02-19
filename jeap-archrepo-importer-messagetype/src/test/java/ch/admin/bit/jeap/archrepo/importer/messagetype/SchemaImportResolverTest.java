package ch.admin.bit.jeap.archrepo.importer.messagetype;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaImportResolverTest {

    @Test
    @SneakyThrows
    void resolve_schemas_with_multiple_imports() {

        File schema = ResourceUtils.getFile("classpath:schema-import-resolver-registry/descriptor/wvs/event/wvsjourneyturnedbackevent/WvsJourneyTurnedBackEvent_v1.avdl");

        String resolvedSchema = SchemaImportResolver.resolveImportsFromSchema(schema);

        assertContent("WvsJourneyTurnedBackEvent_v1.expected", resolvedSchema);

    }

    @Test
    @SneakyThrows
    void resolve_schemas_with_multiple_recursive_imports() {

        File schema = ResourceUtils.getFile("classpath:schema-import-resolver-registry/descriptor/autorisaziun/command/autorisaziunvalidatepermitv2command/AutorisaziunValidatePermitV2Command_v2.2.0.avdl");

        String resolvedSchema = SchemaImportResolver.resolveImportsFromSchema(schema);

        assertContent("AutorisaziunValidatePermitV2Command_v2.expected", resolvedSchema);

    }

    private static void assertContent(String expectationResourceName, String actualContent) throws IOException {
        assertThat(actualContent).isEqualToIgnoringWhitespace(loadExpectation("resolved-schemas/" + expectationResourceName));
    }

    private static String loadExpectation(String name) throws IOException {
        try (InputStream stream = new ClassPathResource(name).getInputStream()) {
            return StreamUtils.copyToString(stream, StandardCharsets.UTF_8).trim();
        }
    }

    @Test
    void getBasePathFromSchemaPath(){

        assertThat(
                SchemaImportResolver.getBasePathFromSchemaPath("dazit-message-type-registry/descriptor/boga/_common/ch.admin.bazg.borderguard.common.v1.AnprBundle.avdl"))
                .isEqualTo("dazit-message-type-registry/descriptor/boga");

        assertThat(
                SchemaImportResolver.getBasePathFromSchemaPath("dazit-message-type-registry/descriptor/boga/event/bogaactivationzoneenteredevent/BogaActivationZoneEnteredEvent_v1.4.1.avdl"))
                .isEqualTo("dazit-message-type-registry/descriptor/boga");

    }



}