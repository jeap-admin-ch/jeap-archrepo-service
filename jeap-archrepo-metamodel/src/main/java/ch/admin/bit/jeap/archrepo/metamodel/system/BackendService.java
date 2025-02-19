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
@DiscriminatorValue("BACKEND_SERVICE")
public class BackendService extends SystemComponent {

    BackendService(String name) {
        super(name);
    }

    @Override
    public SystemComponentType getType() {
        return SystemComponentType.BACKEND_SERVICE;
    }

}
