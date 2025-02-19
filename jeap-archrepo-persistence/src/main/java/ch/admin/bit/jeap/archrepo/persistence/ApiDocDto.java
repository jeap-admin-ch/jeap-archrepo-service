package ch.admin.bit.jeap.archrepo.persistence;

import java.time.ZonedDateTime;

public interface ApiDocDto {
    String getServerUrl();
    String getVersion();
    ZonedDateTime getCreatedAt();
    ZonedDateTime getModifiedAt();
}
