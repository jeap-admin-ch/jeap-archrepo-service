package ch.admin.bit.jeap.archrepo.model.database;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseSchemaTest {

    @Test
    void testSerializationAndDeserialization() throws Exception {
        Table tableA = Table.builder()
                .name("table_foo")
                .columns(List.of(
                        new TableColumn("col_a", "text", false),
                        new TableColumn("col_b", "bytea", false),
                        new TableColumn("col_c", "text", true)))
                .primaryKey(new TablePrimaryKey("pk_foo", List.of("col_a")))
                .build();
        Table tableB = Table.builder()
                .name("table_bar")
                .columns(List.of(
                        new TableColumn("ref_col_a", "text", false),
                        new TableColumn("col_d", "text", true)))
                .foreignKeys(List.of(
                        TableForeignKey.builder()
                            .name("fk_foo_bar")
                            .columnNames(List.of("ref_col_a"))
                            .referencedColumnNames(List.of("col_a"))
                            .build()))
                .build();
        DatabaseSchema databaseSchema = DatabaseSchema.builder()
                .name("test-schema")
                .version("1.2.3")
                .tables(List.of(tableA, tableB))
                .build();


        byte[] serializedSchma = databaseSchema.toJson();
        DatabaseSchema deserializedSchema = DatabaseSchema.fromJson(serializedSchma);

        assertEquals(databaseSchema, deserializedSchema);
    }

}
