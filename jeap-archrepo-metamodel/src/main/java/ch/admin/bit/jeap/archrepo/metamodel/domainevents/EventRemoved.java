package ch.admin.bit.jeap.archrepo.metamodel.domainevents;

import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import lombok.Value;

@Value
public class EventRemoved {
    String eventName;

    public static EventRemoved of(Event event) {
        return new EventRemoved(event.getMessageTypeName());
    }
}
