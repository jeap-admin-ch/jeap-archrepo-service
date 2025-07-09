package ch.admin.bit.jeap.archrepo.docgen.plantuml;

import ch.admin.bit.jeap.archrepo.model.database.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class PlantUmlDbSchemaRenderer {

    // Table names (in lowercase) that should not be rendered
    private static final Set<String> NON_RENDERED_TABLE_NAMES = Set.of(
            "flyway_schema_history",
            "shedlock"
    );

    public String renderDbSchema(DatabaseSchema dbSchema) {
        StringBuilder plantUml = new StringBuilder();

        plantUml.append("@startuml\n");
        plantUml.append("title ").append(dbSchema.name()).append(" (").append(dbSchema.version()).append(")\n");
        plantUml.append("!theme mars\n");
        plantUml.append("skinparam linetype curved\n\n");

        // Render tables
        streamTables(dbSchema).forEach(table ->
            renderTable(plantUml, table));

        plantUml.append("\n");

        // Render relationships
        streamTables(dbSchema).forEach(table ->
            renderRelationships(plantUml, table));

        plantUml.append("@enduml\n");
        
        return plantUml.toString();
    }

    private Stream<Table> streamTables(DatabaseSchema dbSchema) {
        return dbSchema.tables().stream().filter(this::shouldRender);
    }

    @SuppressWarnings("ConstantValue")
    private void renderTable(StringBuilder plantUml, Table table) {
        plantUml.append("entity \"").append(table.name()).append("\" {\n");
        
        List<String> pkColumns = Optional.ofNullable(table.primaryKey()).
                map(TablePrimaryKey::columnNames).
                filter(Objects::nonNull).
                orElse(List.of());
        Set<String> fkColumns = Optional.ofNullable(table.foreignKeys()).orElse(List.of()).stream().
                map(TableForeignKey::columnNames).
                filter(Objects::nonNull).
                flatMap(List::stream).
                collect(Collectors.toSet());

        // Render primary key columns first
        table.columns().stream()
            .filter(column -> pkColumns.contains(column.name()))
            .forEach(column ->
                    renderColumn(plantUml, column, true, fkColumns.contains(column.name())));

        if (!pkColumns.isEmpty() && !(table.columns().size() == pkColumns.size())) {
            plantUml.append("    --\n");
        }

        // Render non-primary key columns
        table.columns().stream()
                .filter(column -> !pkColumns.contains(column.name()))
                .forEach(column ->
                        renderColumn(plantUml, column, false, fkColumns.contains(column.name())));

        plantUml.append("}\n\n");
    }

    private void renderColumn(StringBuilder plantUml, TableColumn column, boolean isPrimaryKey, boolean isForeignKey) {
        plantUml.append("  ");
        if (column.nullable() || isPrimaryKey) {
            plantUml.append("* ");
        } else {
            plantUml.append("  ");
        }
        plantUml.append(column.name()).append(" : ").append(column.type());
        if (isPrimaryKey) {
            plantUml.append(" <<PK>>");
        }
        if (isForeignKey) {
            plantUml.append(" <<FK>>");
        }
        plantUml.append("\n");
    }
    
    private void renderRelationships(StringBuilder plantUml, Table table) {
        if (table.foreignKeys() != null) {
            for (TableForeignKey foreignKey : table.foreignKeys()) {
                plantUml.append("\"").append(table.name()).append("\" }o--|| \"")
                        .append(foreignKey.referencedTableName()).append("\" : ")
                        .append(String.join(", ", foreignKey.columnNames()))
                        .append("\n");
            }
        }
    }

    boolean shouldRender(Table table) {
        return table.name() != null && !NON_RENDERED_TABLE_NAMES.contains(table.name().toLowerCase());
    }

}

