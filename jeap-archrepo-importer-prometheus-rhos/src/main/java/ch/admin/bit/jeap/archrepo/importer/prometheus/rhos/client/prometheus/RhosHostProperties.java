package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
public class RhosHostProperties {

    private String host;

    @ToString.Exclude
    private String serviceAccountToken;

}