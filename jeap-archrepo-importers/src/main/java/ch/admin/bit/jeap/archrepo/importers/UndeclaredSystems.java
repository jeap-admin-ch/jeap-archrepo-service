package ch.admin.bit.jeap.archrepo.importers;

import org.slf4j.Logger;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Collects what an importer skipped during one run because its system is not declared in {@code archrepo.model},
 * so that the gap is reported once per run instead of once per element.
 * <p>
 * An undeclared system is a configuration gap, not a failure of the run: logging every skipped element - the
 * deployment log alone reports some components dozens of times - buried the real errors. One WARN per run still
 * names every gap and can be searched or alerted on. Create a new instance for every run.
 */
public class UndeclaredSystems {

    private final Map<String, SortedSet<String>> skippedElementsBySystem = new TreeMap<>();

    /**
     * Records a system that was skipped as a whole, e.g. an application or namespace named after it.
     */
    public void skip(String systemName) {
        skippedElementsBySystem.computeIfAbsent(systemName, name -> new TreeSet<>());
    }

    /**
     * Records an element (component, message type, ...) that was skipped because its system is not declared.
     */
    public void skip(String systemName, String elementName) {
        skippedElementsBySystem.computeIfAbsent(systemName, name -> new TreeSet<>()).add(elementName);
    }

    public boolean isEmpty() {
        return skippedElementsBySystem.isEmpty();
    }

    /**
     * Logs one WARN listing every skipped system with its skipped elements, sorted and without duplicates.
     * Logs nothing if nothing was skipped.
     */
    public void logSummary(Logger log, String source) {
        if (isEmpty()) {
            return;
        }
        log.warn("{}: skipped {} systems not declared in archrepo.model: {}",
                source, skippedElementsBySystem.size(), describe());
    }

    String describe() {
        return skippedElementsBySystem.entrySet().stream()
                .map(entry -> entry.getValue().isEmpty()
                        ? entry.getKey()
                        : entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
