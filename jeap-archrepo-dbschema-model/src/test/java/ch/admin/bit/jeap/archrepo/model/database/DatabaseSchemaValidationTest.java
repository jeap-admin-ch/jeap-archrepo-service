package ch.admin.bit.jeap.archrepo.model.database;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation has to cascade from the schema into its tables and from the tables into their columns and foreign keys.
 */
class DatabaseSchemaValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validSchema_hasNoViolations() {
        assertThat(validator.validate(schemaWith(validTable()))).isEmpty();
    }

    @Test
    void tableWithoutColumns_isInvalid() {
        Table table = Table.builder().name("table_foo").columns(List.of()).build();

        assertThat(violationPaths(validator.validate(schemaWith(table))))
                .containsExactly("tables[0].columns");
    }

    @Test
    void columnWithBlankName_isInvalid() {
        Table table = Table.builder()
                .name("table_foo")
                .columns(List.of(new TableColumn("", "text", false)))
                .build();

        assertThat(violationPaths(validator.validate(schemaWith(table))))
                .containsExactly("tables[0].columns[0].name");
    }

    @Test
    void foreignKeyWithoutName_isInvalid() {
        Table table = Table.builder()
                .name("table_foo")
                .columns(List.of(new TableColumn("col_a", "text", false)))
                .foreignKeys(List.of(TableForeignKey.builder()
                        .columnNames(List.of("col_a"))
                        .referencedTableName("table_bar")
                        .referencedColumnNames(List.of("col_b"))
                        .build()))
                .build();

        assertThat(violationPaths(validator.validate(schemaWith(table))))
                .containsExactly("tables[0].foreignKeys[0].name");
    }

    private static Table validTable() {
        return Table.builder()
                .name("table_foo")
                .columns(List.of(new TableColumn("col_a", "text", false)))
                .build();
    }

    private static DatabaseSchema schemaWith(Table table) {
        return DatabaseSchema.builder()
                .name("test-schema")
                .version("1.2.3")
                .tables(List.of(table))
                .build();
    }

    private static List<String> violationPaths(Set<ConstraintViolation<DatabaseSchema>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .toList();
    }
}
