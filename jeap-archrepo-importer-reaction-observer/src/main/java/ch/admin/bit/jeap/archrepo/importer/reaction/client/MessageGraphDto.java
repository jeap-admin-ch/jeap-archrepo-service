package ch.admin.bit.jeap.archrepo.importer.reaction.client;

import java.util.HashMap;
import java.util.Set;

public class MessageGraphDto extends HashMap<String, GraphDto> {

    public Set<String> getVariants() {
        return this.keySet();
    }

}
