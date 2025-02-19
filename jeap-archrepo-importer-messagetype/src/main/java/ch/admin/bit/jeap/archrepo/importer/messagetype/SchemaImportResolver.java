package ch.admin.bit.jeap.archrepo.importer.messagetype;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
@Slf4j
public class SchemaImportResolver {
    private static final String RECORD_PREFIX = "record ";
    private static final String PROTOCOL = "protocol";
    private static final List<String> IGNORED_SCHEMAS = List.of("MessagingBaseTypes.avdl", "DomainEventBaseTypes.avdl");
    private static final String IMPORT_IDL_PREFIX = "import idl \"";

    public static String resolveImportsFromSchema(File pathToSchema) {

        try {

            File basePath = new File(getBasePathFromSchemaPath(pathToSchema.getAbsolutePath()));
            Set<String> importedSchemasNames = new HashSet<>();
            List<StringBuilder> importedSchemas = new ArrayList<>();
            StringBuilder schema = new StringBuilder();
            List<String> allLines = removeEmptyLinesAtTheEnd(Files.readAllLines(pathToSchema.toPath()));

            for (String nextLine : allLines.subList(0, allLines.size() - 1)) {
                if (isImportLine(nextLine)) {
                    resolveImport(basePath, importedSchemasNames, importedSchemas, nextLine);
                } else {
                    appendWithNewLine(schema, removeProtocolPrefixFromMainSchema(nextLine));
                }
            }
            return importedSchemas.stream().map(StringBuilder::toString).collect(Collectors.joining()) + schema;
        } catch (IOException e) {
            log.error("Error resolving import in schema {}", pathToSchema, e);
            throw new IllegalStateException(e);
        }
    }


    private static void resolveImport(File basePath, Set<String> importedSchemasNames, List<StringBuilder> importedSchemas, String lineWithImport) throws IOException {
        String importFilename = readFilenameFromImportLine(lineWithImport);

        if (!importedSchemasNames.contains(importFilename)) {
            importedSchemasNames.add(importFilename);
            List<Path> imports = findByFilenameInPath(basePath.toPath(), importFilename);

            if (imports.size() == 1) {

                StringBuilder builder = new StringBuilder();

                List<String> allLines = removeEmptyLinesAtTheEnd(Files.readAllLines(imports.getFirst()));

                appendWithNewLine(builder, "//-- Start imported schema " + importFilename);
                for (String line : allLines.subList(0, allLines.size() - 1)) {
                    if (isImportLine(line)) {
                        resolveImport(basePath, importedSchemasNames, importedSchemas, line);
                    } else if (!line.trim().isEmpty() && !line.trim().startsWith(PROTOCOL)) {
                        appendWithNewLine(builder, line);
                    }
                }

                appendWithNewLine(builder, "//-- End imported schema");
                appendWithNewLine(builder, "");
                importedSchemas.add(builder);
            } else if (imports.size() > 1) {
                log.warn("Found {} files for schema {}", imports.size(), importFilename);
            } else if (!IGNORED_SCHEMAS.contains(importFilename)) {
                log.warn("No file found for schema {}", importFilename);
            }

        }
    }

    private static List<String> removeEmptyLinesAtTheEnd(List<String> lines) {
        int cursor = lines.size() - 1;
        while (lines.get(cursor).trim().isEmpty()){
            lines.remove(cursor);
            cursor--;
        }
        return lines;
    }

    private static boolean isImportLine(String line) {
        return line.trim().startsWith(IMPORT_IDL_PREFIX);
    }

    private static String readFilenameFromImportLine(String line) {
        String toImport = line.trim().replace(IMPORT_IDL_PREFIX, "");
        toImport = toImport.replace(";", "");
        return toImport.replace("\"", "");
    }

    private static void appendWithNewLine(StringBuilder builder, String line) {
        if (!line.trim().startsWith("@namespace")) {
            builder.append(removeRecordPrefix(line));
            builder.append(System.getProperty("line.separator"));
        }
    }

    private static String removeRecordPrefix(String input) {
        if (input.trim().startsWith(RECORD_PREFIX)) {
            input = input.replace(RECORD_PREFIX, "");
        }
        return input;
    }

    private static String removeProtocolPrefixFromMainSchema(String input) {
        if (input.trim().startsWith(PROTOCOL)) {
            String line = input.replace(PROTOCOL, "//-- Start");
            return line.replace("Protocol {", "");
        }
        return input;
    }

    private static List<Path> findByFilenameInPath(Path path, String filename) throws IOException {
        List<Path> result;

        try (Stream<Path> pathStream = Files.find(path,
                Integer.MAX_VALUE,
                (p, basicFileAttributes) -> {
                    // if directory or no-read permission, ignore
                    if (Files.isDirectory(p) || !Files.isReadable(p)) {
                        return false;
                    }
                    return p.getFileName().toString().equalsIgnoreCase(filename);
                })
        ) {
            result = pathStream.toList();
        }
        return result;
    }

    public static String getBasePathFromSchemaPath(String schemaAbsolutePath) {

        if (schemaAbsolutePath.contains("/_common/")) {
            return schemaAbsolutePath.split("/_common/")[0];
        }

        if (schemaAbsolutePath.contains("/event/")) {
            return schemaAbsolutePath.split("/event/")[0];
        }

        if (schemaAbsolutePath.contains("/command/")) {
            return schemaAbsolutePath.split("/command/")[0];
        }

        throw new IllegalStateException("SchemaAbsolutePath " + schemaAbsolutePath + " is not a valid path");
    }

}
