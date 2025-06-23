package ch.admin.bit.jeap.archrepo.metamodel.reaction;

import ch.admin.bit.jeap.archrepo.metamodel.MutableDomainEntity;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
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
    @Setter
    private SystemComponent component;

    private String triggerType;

    private String triggerFqn;

    private int count;

    private double median;

    private double percentage;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "reactionStatistics")
    private List<ActionEntity> actions = new ArrayList<>();

    public void addAction(ActionEntity action) {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        action.setReactionStatistics(this);
        actions.add(action);
    }

    protected ReactionStatistics() {super();}

    @Builder
    public ReactionStatistics(SystemComponent component,
                              String triggerType,
                              String triggerFqn,
                              int count,
                              double median,
                              double percentage) {
        this.id = Generators.timeBasedEpochGenerator().generate();
        this.component = component;
        this.triggerType = triggerType;
        this.triggerFqn = triggerFqn;
        this.count = count;
        this.median = median;
        if (triggerType != null) {
            this.percentage = percentage;
        }
    }
}
