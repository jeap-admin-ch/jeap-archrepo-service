package ch.admin.bit.jeap.archrepo.metamodel.message;

import java.util.Comparator;

/**
 * Orders the version strings of a {@link MessageVersion} the way a reader expects: {@code 2.0.0} before
 * {@code 10.0.0}.
 * <p>
 * Message type versions are dot-separated numbers, so comparing them as text would put every two-digit
 * component in the wrong place - and a documentation page that lists them is read by people. It lives in the
 * metamodel because both the model export and the message type resources of the docs API order the same
 * strings, and two orders for one fact is worse than either order.
 * <p>
 * A component the registry allows but the importers do not produce - {@code 1.0.0-rc1} - must not fail a
 * request, so a component is compared as its numeric prefix and then as the text that follows it. That keeps the
 * comparison <b>total and transitive</b>, which a numeric-or-text choice per pair would not be: with
 * {@code 10}, {@code 1a} and {@code 2}, comparing some pairs as numbers and others as text yields a cycle, and
 * {@code List.sort} rejects it at runtime.
 */
public final class MessageVersionOrder implements Comparator<String> {

    public static final Comparator<String> INSTANCE = new MessageVersionOrder();

    /** A component with no leading digits, so that it orders consistently against every numeric one. */
    private static final long NOT_A_NUMBER = -1;

    private MessageVersionOrder() {
    }

    @Override
    public int compare(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        for (int i = 0; i < Math.max(leftParts.length, rightParts.length); i++) {
            int comparison = comparePart(part(leftParts, i), part(rightParts, i));
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    /**
     * A missing component counts as zero, so {@code 1.0} and {@code 1.0.0} are the same version.
     */
    private static String part(String[] parts, int index) {
        return index < parts.length ? parts[index] : "0";
    }

    /**
     * The numeric prefix decides, the rest breaks the tie: {@code 2} before {@code 10}, {@code 1} before
     * {@code 1a}, and {@code a} - which has no number at all - before both.
     */
    private static int comparePart(String left, String right) {
        int byNumber = Long.compare(numericPrefixOf(left), numericPrefixOf(right));
        return byNumber != 0 ? byNumber : left.compareTo(right);
    }

    private static long numericPrefixOf(String part) {
        int digits = 0;
        while (digits < part.length() && Character.isDigit(part.charAt(digits))) {
            digits++;
        }
        if (digits == 0) {
            return NOT_A_NUMBER;
        }
        try {
            return Long.parseLong(part.substring(0, digits));
        } catch (NumberFormatException e) {
            // More digits than a long holds; such a version does not exist, and text order is a fine answer
            return Long.MAX_VALUE;
        }
    }
}
