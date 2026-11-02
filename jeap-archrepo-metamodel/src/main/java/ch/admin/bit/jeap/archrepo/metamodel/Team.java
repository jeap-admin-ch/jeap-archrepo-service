package ch.admin.bit.jeap.archrepo.metamodel;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@NoArgsConstructor
@Entity
@Getter
public class Team extends MutableDomainEntity implements Persistable<UUID> {

    @Transient
    // Assigned UUIDs must not make a newly configured team look detached to Spring Data.
    private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    private void markPersisted() {
        newEntity = false;
    }

    @Id
    @NotNull
    private UUID id;

    @NotNull
    String name;

    String contactAddress;

    String confluenceLink;

    String jiraLink;

    @Builder
    public Team(String name, String contactAddress, String confluenceLink, String jiraLink) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.contactAddress = contactAddress;
        this.confluenceLink = confluenceLink;
        this.jiraLink = jiraLink;
    }

    public void updateName(String name) {
        if (!this.name.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("Team renames are not supported: " + this.name + " -> " + name);
        }
        this.name = name;
    }

    public void updateDetails(String contactAddress, String confluenceLink, String jiraLink) {
        this.contactAddress = contactAddress;
        this.confluenceLink = confluenceLink;
        this.jiraLink = jiraLink;
    }
}
