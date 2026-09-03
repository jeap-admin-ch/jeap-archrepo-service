package ch.admin.bit.jeap.archrepo.metamodel.system;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
@Entity
@DiscriminatorValue("GATEWAY")
public class Gateway extends SystemComponent {

    Gateway(String name) {
        super(name);
    }

    @Override
    public SystemComponentType getType() {
        return SystemComponentType.GATEWAY;
    }
}
