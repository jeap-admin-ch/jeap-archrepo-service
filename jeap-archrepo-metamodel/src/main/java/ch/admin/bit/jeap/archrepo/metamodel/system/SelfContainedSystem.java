package ch.admin.bit.jeap.archrepo.metamodel.system;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@Entity
@DiscriminatorValue("SELF_CONTAINED_SYSTEM")
public class SelfContainedSystem extends SystemComponent {

    SelfContainedSystem(String name) {
        super(name);
    }

    @Override
    public SystemComponentType getType() {
        return SystemComponentType.SELF_CONTAINED_SYSTEM;
    }
}
