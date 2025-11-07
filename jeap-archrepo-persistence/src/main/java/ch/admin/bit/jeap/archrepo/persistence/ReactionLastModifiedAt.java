package ch.admin.bit.jeap.archrepo.persistence;

import java.time.ZonedDateTime;

public interface ReactionLastModifiedAt {
    String getComponent();
    ZonedDateTime getMaxCreatedAt();
    ZonedDateTime getMaxModifiedAt();

}