package ch.admin.bit.jeap.archrepo.persistence;

import java.time.ZonedDateTime;

public interface ReactionStatisticsLastModifiedAt {
    String getComponent();
    ZonedDateTime getLastModifiedAt();
}
