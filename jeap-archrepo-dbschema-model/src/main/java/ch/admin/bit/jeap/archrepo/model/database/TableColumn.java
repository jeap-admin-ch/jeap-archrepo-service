package ch.admin.bit.jeap.archrepo.model.database;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record TableColumn(
        @NotEmpty String name,
        @NotEmpty String type,
        boolean nullable)
{}
