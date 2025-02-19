package ch.admin.bit.jeap.archrepo.metamodel;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@NoArgsConstructor
@Entity
@Getter
public class Team extends MutableDomainEntity {

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
}
