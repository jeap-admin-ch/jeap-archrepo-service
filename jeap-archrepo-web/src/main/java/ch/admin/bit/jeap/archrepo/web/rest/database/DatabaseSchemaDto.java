package ch.admin.bit.jeap.archrepo.web.rest.database;

import ch.admin.bit.jeap.archrepo.model.database.DatabaseSchema;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
public class DatabaseSchemaDto {
    private String systemComponentName;
    @ToString.Exclude
    DatabaseSchema schema;
}

