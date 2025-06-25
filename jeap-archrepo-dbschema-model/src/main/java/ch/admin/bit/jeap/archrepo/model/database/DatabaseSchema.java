package ch.admin.bit.jeap.archrepo.model.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.io.IOException;
import java.util.List;

@Builder
public record DatabaseSchema(
        @NotEmpty String name,
        @NotEmpty String version,
        @NotEmpty @Valid List<Table> tables)
{
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    public byte[] toJson() throws JsonProcessingException {
            return MAPPER.writeValueAsBytes(this);
    }

    public static DatabaseSchema fromJson(byte[] json) throws IOException {
            return MAPPER.readValue(json, DatabaseSchema.class);
    }

}





