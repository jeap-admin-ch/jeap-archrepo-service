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
@DiscriminatorValue("MOBILE_APP")
public class MobileApp extends SystemComponent {

    MobileApp(String name) {
        super(name);
    }

    @Override
    public SystemComponentType getType() {
        return SystemComponentType.MOBILE_APP;
    }
}
