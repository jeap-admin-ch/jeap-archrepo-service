package ch.admin.bit.jeap.archrepo.metamodel;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ContentHashTest {

    @Test
    void of_knownValue() {
        // The SHA-256 of "abc", the standard test vector
        assertThat(ContentHash.of("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void of_isStableForEqualContent() {
        byte[] one = "the same bytes".getBytes(StandardCharsets.UTF_8);
        byte[] other = "the same bytes".getBytes(StandardCharsets.UTF_8);

        assertThat(ContentHash.of(one)).isEqualTo(ContentHash.of(other));
    }

    @Test
    void of_changesWithContent() {
        assertThat(ContentHash.of("one".getBytes(StandardCharsets.UTF_8)))
                .isNotEqualTo(ContentHash.of("two".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void of_isLowercaseHexOfFixedLength() {
        assertThat(ContentHash.of("anything".getBytes(StandardCharsets.UTF_8)))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }

    @Test
    void of_emptyContent() {
        assertThat(ContentHash.of(new byte[0]))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void of_null_isNull() {
        assertThat(ContentHash.of(null)).isNull();
    }
}
