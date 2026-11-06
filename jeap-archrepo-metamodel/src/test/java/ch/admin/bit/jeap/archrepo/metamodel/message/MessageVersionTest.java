package ch.admin.bit.jeap.archrepo.metamodel.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MessageVersionTest {

    @Test
    void contentHash_isComputedWhenTheVersionIsBuilt() {
        assertThat(full().build().getContentHash()).matches("[0-9a-f]{64}");
    }

    @Test
    void contentHash_isStableForEqualFields() {
        assertThat(full().build().getContentHash()).isEqualTo(full().build().getContentHash());
    }

    /**
     * Every field the docs API serves for a version has to move the hash, or a consumer comparing tags would keep
     * a version that changed. The compatible version is the one that changes without the version itself changing.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("changedFields")
    void contentHash_changesWithEveryServedField(String field,
                                                 UnaryOperator<MessageVersion.MessageVersionBuilder> change) {
        String original = full().build().getContentHash();

        assertThat(change.apply(full()).build().getContentHash()).isNotEqualTo(original);
    }

    static Stream<Arguments> changedFields() {
        return Stream.of(
                Arguments.of("version", op(b -> b.version("2.0.1"))),
                Arguments.of("compatibilityMode", op(b -> b.compatibilityMode("FORWARD"))),
                Arguments.of("compatibleVersion", op(b -> b.compatibleVersion("1.5.0"))),
                Arguments.of("keySchemaName", op(b -> b.keySchemaName("OtherKey.avdl"))),
                Arguments.of("keySchemaUrl", op(b -> b.keySchemaUrl("https://registry.example.com/Other.avdl"))),
                Arguments.of("keySchemaResolved", op(b -> b.keySchemaResolved("other key schema"))),
                Arguments.of("valueSchemaName", op(b -> b.valueSchemaName("OtherValue.avdl"))),
                Arguments.of("valueSchemaUrl", op(b -> b.valueSchemaUrl("https://registry.example.com/V.avdl"))),
                Arguments.of("valueSchemaResolved", op(b -> b.valueSchemaResolved("other value schema"))));
    }

    @Test
    void contentHash_tellsAnAbsentKeySchemaFromABlankOne() {
        assertThat(full().keySchemaName(null).build().getContentHash())
                .isNotEqualTo(full().keySchemaName("").build().getContentHash());
    }

    @Test
    void contentHash_followsTheSetters() {
        MessageVersion version = full().keySchemaName(null).keySchemaUrl(null).build();
        String before = version.getContentHash();

        version.setKeySchemaName("Key.avdl");
        String afterName = version.getContentHash();
        version.setKeySchemaUrl("https://registry.example.com/Key.avdl");

        assertThat(afterName).isNotEqualTo(before);
        assertThat(version.getContentHash()).isNotEqualTo(afterName);
    }

    private static UnaryOperator<MessageVersion.MessageVersionBuilder> op(
            UnaryOperator<MessageVersion.MessageVersionBuilder> change) {
        return change;
    }

    private static MessageVersion.MessageVersionBuilder full() {
        return MessageVersion.builder()
                .version("2.0.0")
                .compatibilityMode("BACKWARD")
                .compatibleVersion("1.0.0")
                .keySchemaName("Key.avdl")
                .keySchemaUrl("https://registry.example.com/Key.avdl")
                .keySchemaResolved("key schema")
                .valueSchemaName("Value.avdl")
                .valueSchemaUrl("https://registry.example.com/Value.avdl")
                .valueSchemaResolved("value schema");
    }
}
