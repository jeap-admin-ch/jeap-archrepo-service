package ch.admin.bit.jeap.archrepo.metamodel.restapi;

import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenApiSpecTest {

    private static final byte[] CONTENT = "{\"openapi\":\"3.0.0\"}".getBytes();

    @Test
    void contentHash_isSetOnConstruction() {
        assertThat(createOpenApiSpec().getContentHash()).isEqualTo(ContentHash.of(CONTENT));
    }

    @Test
    void contentHash_followsUpdate() {
        OpenApiSpec spec = createOpenApiSpec();
        byte[] newContent = "{\"openapi\":\"3.1.0\"}".getBytes();

        spec.update(newContent, "2.0.0", "https://foo-bar.example.com");

        assertThat(spec.getContentHash())
                .isEqualTo(ContentHash.of(newContent))
                .isNotEqualTo(ContentHash.of(CONTENT));
    }

    private OpenApiSpec createOpenApiSpec() {
        SystemComponent provider = mock(SystemComponent.class);
        when(provider.getParent()).thenReturn(mock(System.class));
        return OpenApiSpec.builder()
                .provider(provider)
                .content(CONTENT)
                .version("1.0.0")
                .serverUrl("https://foo-bar.example.com")
                .build();
    }
}
