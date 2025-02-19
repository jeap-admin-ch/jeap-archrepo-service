package ch.admin.bit.jeap.archrepo.metamodel.domainevents;

import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import lombok.Value;

@Value
public class CommandRemoved {
    String commandName;

    public static CommandRemoved of(Command command) {
        return new CommandRemoved(command.getMessageTypeName());
    }

}
