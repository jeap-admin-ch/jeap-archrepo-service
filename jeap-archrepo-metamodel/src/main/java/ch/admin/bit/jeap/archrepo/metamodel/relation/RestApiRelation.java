package ch.admin.bit.jeap.archrepo.metamodel.relation;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@EqualsAndHashCode(callSuper = true)
// restApi stays out: printing it would nest a whole RestApi in every relation log line
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Getter
@Entity
@DiscriminatorValue("REST_API")
public class RestApiRelation extends AbstractRelation {

    @Setter
    @EqualsAndHashCode.Exclude
    @ToString.Include
    private String pactUrl;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    private RestApi restApi;

    @Setter
    @EqualsAndHashCode.Exclude
    @ToString.Include
    private ZonedDateTime lastSeen;

    @Override
    public String getLabel() {
        return restApi.getMethod() + " " + restApi.getPath();
    }

    @Override
    public boolean isLabelLinkable() {
        return false;
    }

    @Override
    public RelationType getType() {
        return RelationType.REST_API_RELATION;
    }

    @Override
    public String getProviderName() {
        return restApi == null ? null : restApi.getProvider().getName();
    }

    @Builder
    public RestApiRelation(System definingSystem, String providerName, String consumerName, Importer importer, String pactUrl, RestApi restApi, ZonedDateTime lastSeen) {
        super(definingSystem, providerName, consumerName, importer);
        this.pactUrl = pactUrl;
        this.restApi = restApi;
        this.lastSeen = lastSeen;
    }
}
