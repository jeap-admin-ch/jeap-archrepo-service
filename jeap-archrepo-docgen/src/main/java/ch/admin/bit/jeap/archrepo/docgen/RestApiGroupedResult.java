package ch.admin.bit.jeap.archrepo.docgen;

import java.util.List;

public record RestApiGroupedResult(
        RestApiKey key,
        String method,
        List<ProvidedRestAPIRelationView> relations) {
}
