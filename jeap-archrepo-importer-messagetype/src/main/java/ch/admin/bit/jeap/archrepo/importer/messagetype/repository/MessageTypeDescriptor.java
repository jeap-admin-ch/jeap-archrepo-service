package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import java.util.List;

public interface MessageTypeDescriptor {

    String getDefiningSystem();

    String getMessageTypeName();

    String getMessageTypeSubdir();

    List<MessageTypeVersion> getVersions();
}
