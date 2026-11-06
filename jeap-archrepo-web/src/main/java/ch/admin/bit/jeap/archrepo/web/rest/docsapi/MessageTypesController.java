package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.persistence.MessageTypeVersionDetail;
import ch.admin.bit.jeap.archrepo.persistence.MessageTypeVersionIndexEntry;
import ch.admin.bit.jeap.archrepo.persistence.MessageTypeVersionRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The message type resources of the docs API: which message types exist in which versions, and everything the
 * arch repo knows about one of those versions - the Avro schemas included.
 * <p>
 * A version is <b>practically</b> stable - a changed schema is published as a new version - but not immutable:
 * {@code compatibleVersion} is derived from the version list when the descriptor does not declare one, so
 * publishing an intermediate version changes what an existing version answers, and a re-import re-renders the
 * schemas. Both resources therefore revalidate like every other resource of this API rather than promising a
 * cache lifetime the importer does not guarantee.
 * <p>
 * What makes that cheap is the stored hash of each version: every model update re-imports the message types and
 * writes it over everything the version resource serves, the compatible version included. The index lists the
 * entity tag derived from it next to each version, so a consumer compares tags instead of asking about every
 * version, and the version resource answers a conditional request from the same tag without reading the schemas.
 */
@RestController
@RequestMapping(value = DocsApiPaths.MESSAGE_TYPES, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
// Overrides the global list, which also offers basic auth: this API answers 401 to basic credentials
@SecurityRequirement(name = "OIDC")
@Tag(name = "docs-api-message-types", description = "The message types and their Avro schemas")
@TransactionalReadReplica
class MessageTypesController {

    private final MessageTypeVersionRepository messageTypeVersionRepository;
    private final SystemRepository systemRepository;
    private final DocsApiEtagSupport etagSupport;

    @GetMapping
    @RequiresArchitectureModelRead
    @Operation(summary = "Every message type with its versions",
            description = "The index a consumer diffs against what it has already replicated. Deliberately "
                          + "light: no schemas, so deciding what to fetch never reads one.")
    @ApiResponse(responseCode = "200", description = "The message types",
            content = @Content(schema = @Schema(implementation = MessageTypeIndexDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "401", description = "No or invalid token", content = @Content)
    @ApiResponse(responseCode = "403", description = "The token lacks the architecture-model read role", content = @Content)
    @ApiResponse(responseCode = "404", description = "The system filter names no known system or alias")
    public ResponseEntity<byte[]> getMessageTypes(
            @Parameter(description = "Restrict the index to one system, by name or alias") @RequestParam(required = false) String system,
            WebRequest request) {
        String systemName = StringUtils.hasText(system) ? resolveSystemName(system) : null;
        return etagSupport.respond(request, index(messageTypeVersionRepository.findIndexEntries(systemName)));
    }

    @GetMapping("/{system}/{message}/versions/{version}")
    @RequiresArchitectureModelRead
    @Operation(summary = "One version of one message type, with its schemas",
            description = "Everything a message page shows about a version: both schemas as the arch repo "
                          + "renders them, and the compatibility it declares. The system is matched by name or "
                          + "by alias, ignoring case. The ETag is the one the message type index lists for this "
                          + "version, so a conditional request is answered without reading the schemas.")
    @ApiResponse(responseCode = "200", description = "The version",
            content = @Content(schema = @Schema(implementation = MessageTypeVersionDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "404", description = "No such system, message type or version")
    public ResponseEntity<byte[]> getMessageTypeVersion(@PathVariable("system") String systemName,
                                                        @PathVariable("message") String messageName,
                                                        @PathVariable("version") String version,
                                                        WebRequest request) {
        String resolvedSystemName = resolveSystemName(systemName);
        // The tag first, from the stored hash and without the schemas: a consumer that already holds this version
        // is answered with a 304 that never reads them. Both queries match the same row - see findVersionTags.
        String entityTag = messageTypeVersionRepository.findVersionTags(resolvedSystemName, messageName, version)
                .stream()
                .findFirst()
                .map(tag -> etagSupport.messageVersionTag(resolvedSystemName, tag.message(), tag.contentHash()))
                .orElse(null);
        if (entityTag != null && etagSupport.isNotModified(request, entityTag)) {
            return null;
        }
        MessageTypeVersionDetail detail =
                messageTypeVersionRepository.findVersions(resolvedSystemName, messageName, version).stream()
                        .findFirst()
                        .orElseThrow(() -> DocsApiException.messageTypeVersionNotFound(resolvedSystemName,
                                messageName, version));
        MessageTypeVersionDto body = version(resolvedSystemName, detail);
        // A version stored before its hash was recorded has no stored tag; it is tagged over its body until the
        // next model update writes the hash, and the index lists it without a tag meanwhile.
        return entityTag == null
                ? etagSupport.respond(request, body)
                : etagSupport.respondTagged(entityTag, body);
    }

    /**
     * Groups the flat rows of the query into one entry per message type. The rows arrive sorted, so the first
     * row of a message type decides where its entry stands and the versions keep the order they were read in.
     * <p>
     * The kind is part of the grouping key, so a system that defines an event and a command of the same name
     * yields two entries rather than one entry with the versions of both under whichever kind was read first.
     */
    private MessageTypeIndexDto index(List<MessageTypeVersionIndexEntry> entries) {
        Map<String, List<MessageTypeVersionRefDto>> versionsByMessageType = new LinkedHashMap<>();
        Map<String, MessageTypeVersionIndexEntry> firstRowOf = new LinkedHashMap<>();
        for (MessageTypeVersionIndexEntry entry : entries) {
            String key = entry.getSystem() + "/" + entry.getKind() + "/" + entry.getMessage();
            firstRowOf.putIfAbsent(key, entry);
            versionsByMessageType.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new MessageTypeVersionRefDto(entry.getVersion(),
                            etagSupport.messageVersionTag(entry.getSystem(), entry.getMessage(),
                                    entry.getContentHash()),
                            DocsApiPaths.messageTypeVersionPath(entry.getSystem(), entry.getMessage(),
                                    entry.getVersion())));
        }

        List<MessageTypeIndexEntryDto> messageTypes = new ArrayList<>();
        firstRowOf.forEach((key, entry) -> messageTypes.add(new MessageTypeIndexEntryDto(
                entry.getSystem(), entry.getMessage(), entry.getKind(), versionsByMessageType.get(key))));
        return new MessageTypeIndexDto(messageTypes);
    }

    private static MessageTypeVersionDto version(String systemName, MessageTypeVersionDetail detail) {
        return new MessageTypeVersionDto(systemName, detail.message(), detail.version(),
                detail.compatibilityMode(), detail.compatibleVersion(),
                schema(detail.keySchemaName(), detail.keySchemaUrl(), detail.keySchemaResolved()),
                schema(detail.valueSchemaName(), detail.valueSchemaUrl(), detail.valueSchemaResolved()));
    }

    private static MessageSchemaDto schema(String schemaName, String schemaUrl, String resolvedSchema) {
        if (schemaName == null && schemaUrl == null && resolvedSchema == null) {
            return null;
        }
        return new MessageSchemaDto(schemaName, schemaUrl, resolvedSchema);
    }

    /**
     * Resolves a system by name or alias, ignoring case
     */
    private String resolveSystemName(String systemName) {
        return systemRepository.findByNameOrAliasIgnoreCase(systemName)
                .map(System::getName)
                .orElseThrow(() -> DocsApiException.systemNotFound(systemName));
    }
}
