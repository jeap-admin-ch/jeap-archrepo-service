package ch.admin.bit.jeap.archrepo.model.database;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Builder
public record DatabaseSchema(
        @NotEmpty String name,
        @NotEmpty String version,
        @NotEmpty @Valid List<Table> tables)
{
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    public byte[] toJson() throws JacksonException {
            return MAPPER.writeValueAsBytes(this);
    }

    public static DatabaseSchema fromJson(byte[] json) throws JacksonException {
            return MAPPER.readValue(json, DatabaseSchema.class);
    }

}





