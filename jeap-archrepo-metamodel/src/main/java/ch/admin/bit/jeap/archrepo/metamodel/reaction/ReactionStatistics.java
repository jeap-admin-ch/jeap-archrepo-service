package ch.admin.bit.jeap.archrepo.metamodel.reaction;

import ch.admin.bit.jeap.archrepo.metamodel.MutableDomainEntity;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@ToString
@Entity
@Getter
public class ReactionStatistics extends MutableDomainEntity {

    @Id
    @NotNull
    private UUID id;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id")
    private SystemComponent component;

    @NotNull
    private String triggerType;

    @NotNull
    private String triggerFqn;

    @NotNull
    private String actionType;

    @NotNull
    private String actionFqn;

    @NotNull
    private Integer count;

    @NotNull
    private Double median;

    @NotNull
    private Double percentage;

    protected ReactionStatistics() {super();}

    @Builder
    public ReactionStatistics(@NonNull SystemComponent component,
                              @NonNull String triggerType,
                              @NonNull String triggerFqn,
                              @NonNull String actionType,
                              @NonNull String actionFqn,
                              @NonNull Integer count,
                              @NonNull Double median,
                              @NonNull Double percentage) {
        this.id = UUID.randomUUID();
        this.component = component;
        this.triggerType = triggerType;
        this.triggerFqn = triggerFqn;
        this.actionType = actionType;
        this.actionFqn = actionFqn;
        this.count = count;
        this.median = median;
        this.percentage = percentage;
    }
}
