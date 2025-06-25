package ch.admin.bit.jeap.archrepo.model.database;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder
public record TableForeignKey(
        @NotEmpty String name,
        @NotEmpty List<String> columnNames,
        @NotEmpty String referencedTableName,
        @NotEmpty List<String> referencedColumnNames)
{}
