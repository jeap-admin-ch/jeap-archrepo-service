package ch.admin.bit.jeap.archrepo.docgen.plantuml;

import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;

import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
class PlantUmlComponent {
    private static final Pattern INVALID_NAME_CHARACTER_PATTERN = Pattern.compile("[^A-Za-z0-9]");

    private final String label;
    private final boolean focus;
    private boolean linkable = true;

    public static PlantUmlComponent of(String label) {
        return new PlantUmlComponent(label, false);
    }

    public static PlantUmlComponent focused(String label) {
        return new PlantUmlComponent(label, true);
    }

    String getName() {
        return componentName(label);
    }

    void removeLink() {
        this.linkable = false;
    }

    void render(StringBuilder uml) {
        uml.append("component \"")
                .append(label)
                .append("\" as ")
                .append(componentName(label))
                .append(' ');
        if (linkable) {
            uml.append("[[./")
                    .append(label) // link to component
                    .append("]] ");
        }
        uml.append(focus()).append('\n');
    }

    private String focus() {
        return focus ? " #Gold" : "";
    }

    static String componentName(String label) {
        return INVALID_NAME_CHARACTER_PATTERN.matcher(label).replaceAll("_");
    }
}
