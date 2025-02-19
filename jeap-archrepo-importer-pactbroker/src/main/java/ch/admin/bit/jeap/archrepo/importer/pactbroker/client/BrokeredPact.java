package ch.admin.bit.jeap.archrepo.importer.pactbroker.client;

import au.com.dius.pact.core.model.Pact;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class BrokeredPact {
    String pactUrl;
    Pact pact;
}
