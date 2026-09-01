package ch.admin.bit.jeap.archrepo.metamodel.message;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MessageVersionOrderTest {

    @Test
    void ordersComponentsAsNumbersRatherThanAsText() {
        assertThat(sorted("2.0.0", "10.0.0", "1.0.0", "1.10.0", "1.2.0"))
                .containsExactly("1.0.0", "1.2.0", "1.10.0", "2.0.0", "10.0.0");
    }

    @Test
    void aMissingComponentCountsAsZero() {
        assertThat(MessageVersionOrder.INSTANCE.compare("1.0", "1.0.0")).isZero();
        assertThat(sorted("1.1", "1.0.5")).containsExactly("1.0.5", "1.1");
    }

    @Test
    void aComponentThatIsNotANumberOrdersByItsNumericPrefixAndThenAsText() {
        assertThat(sorted("1.10.0", "1.1a.0", "1.2.0")).containsExactly("1.1a.0", "1.2.0", "1.10.0");
        assertThat(sorted("1.0.0", "1.0.0-rc1")).containsExactly("1.0.0", "1.0.0-rc1");
    }

    /**
     * A comparison that chose between numbers and text per pair would be intransitive, and TimSort throws on
     * such a comparator once the list is long enough - turning an index into a 500.
     */
    @Test
    void staysTransitiveWhenNumbersAndTextAreMixed() {
        List<String> versions = Arrays.asList("10", "1a", "2", "0", "a", "3", "1", "11", "2b", "20", "9", "b",
                "12", "4", "5", "6", "7", "8", "13", "14", "15", "16", "17", "18", "19", "21", "22", "23", "24",
                "25", "26", "27");

        assertThatCode(() -> versions.sort(MessageVersionOrder.INSTANCE)).doesNotThrowAnyException();
        assertThat(versions).startsWith("a", "b", "0", "1", "1a", "2", "2b", "3");
    }

    private static List<String> sorted(String... versions) {
        return Arrays.stream(versions).sorted(MessageVersionOrder.INSTANCE).toList();
    }
}
