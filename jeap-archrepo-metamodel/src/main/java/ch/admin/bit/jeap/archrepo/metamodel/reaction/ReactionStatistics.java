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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id")
    private SystemComponent component;

    private String triggerType;

    private String triggerFqn;

    private String actionType;

    private String actionFqn;

    private Integer count;

    private Double median;

    private Double percentage;

    protected ReactionStatistics() {super();}

    @Builder
    public ReactionStatistics(SystemComponent component,
                              String triggerType,
                              String triggerFqn,
                              String actionType,
                              String actionFqn,
                              Integer count,
                              Double median,
                              Double percentage) {
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
