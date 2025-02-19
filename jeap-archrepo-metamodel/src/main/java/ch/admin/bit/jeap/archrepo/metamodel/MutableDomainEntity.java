package ch.admin.bit.jeap.archrepo.metamodel;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.ZonedDateTime;

@MappedSuperclass
public abstract class MutableDomainEntity {

    @Getter(value = AccessLevel.PROTECTED)
    protected ZonedDateTime createdAt;

    @Getter(value = AccessLevel.PROTECTED)
    private ZonedDateTime modifiedAt;

    @PreUpdate
    void onPreUpdate() {
        modifiedAt = ZonedDateTime.now();
    }

    @PrePersist
    void onPrePersist() {
        // Allow setting createdAt manually, too
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }

}
