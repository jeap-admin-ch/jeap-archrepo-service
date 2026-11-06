package ch.admin.bit.jeap.archrepo.model.database;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder
public record Table(
        @NotEmpty String name,
        @NotEmpty List<@Valid TableColumn> columns,
        List<@Valid TableForeignKey> foreignKeys,
        @Valid TablePrimaryKey primaryKey)
{}
