package ch.admin.bit.jeap.archrepo.web.rest.database;

import ch.admin.bit.jeap.archrepo.model.database.DatabaseSchema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateOrUpdateDbSchemaDto{
    @NotEmpty
    private String systemName;
    @NotEmpty
    private String systemComponentName;
    @NotNull
    @Valid
    private DatabaseSchema schema;
}

