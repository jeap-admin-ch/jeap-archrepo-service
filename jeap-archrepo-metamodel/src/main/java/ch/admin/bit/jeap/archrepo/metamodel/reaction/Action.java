package ch.admin.bit.jeap.archrepo.metamodel.reaction;

import ch.admin.bit.jeap.archrepo.metamodel.MutableDomainEntity;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@ToString
@Entity
@Getter
public class Action extends MutableDomainEntity {

    @Id
    @NotNull
    private UUID id;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reaction_statistics_id")
    @Setter
    private ReactionStatistics reactionStatistics;

    private String actionType;

    private String actionFqn;

    protected Action() {super();}

    @Builder
    public Action(ReactionStatistics reactionStatistics,
                  String actionType,
                  String actionFqn) {
        this.id = Generators.timeBasedEpochGenerator().generate();
        this.reactionStatistics = reactionStatistics;
        this.actionType = actionType;
        this.actionFqn = actionFqn;
    }
}
