package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.domainevents.CommandRemoved;
import ch.admin.bit.jeap.archrepo.metamodel.domainevents.EventRemoved;
import ch.admin.bit.jeap.archrepo.metamodel.domainevents.SystemComponentRemoved;

import java.util.Set;

public record RemovedElements(Set<SystemComponentRemoved> systemComponentRemovedList,
                              Set<EventRemoved> eventRemovedList,
                              Set<CommandRemoved> commandRemovedList) {
}
