package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One side of a message type version: its Avro schema as the arch repo renders it")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageSchemaDto(
        @Schema(description = "The schema file's name in the message type registry",
                example = "WvsDeclarationAcceptedEventValue.avdl") String schemaName,
        @Schema(description = "Where the file can be browsed in the registry") String schemaUrl,
        @Schema(description = "The schema as a person reads it: the Avro IDL of the file with every imported "
                              + "schema inlined, namespaces and enclosing braces removed and the boundaries "
                              + "marked with comments. It is a rendering produced when the message type was "
                              + "imported, not the file itself, and it is deliberately not valid Avro IDL")
        String resolvedSchema) {
}
