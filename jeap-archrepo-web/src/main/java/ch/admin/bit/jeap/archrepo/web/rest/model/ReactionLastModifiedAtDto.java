package ch.admin.bit.jeap.archrepo.web.rest.model;

import java.time.ZonedDateTime;

public record ReactionLastModifiedAtDto(String component, ZonedDateTime lastModifiedAt) {}