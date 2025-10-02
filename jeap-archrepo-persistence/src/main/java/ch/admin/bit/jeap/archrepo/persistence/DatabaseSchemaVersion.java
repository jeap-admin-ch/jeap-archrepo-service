package ch.admin.bit.jeap.archrepo.persistence;

public interface DatabaseSchemaVersion {
    String getSystem();
    String getComponent();
    String getVersion();
}
