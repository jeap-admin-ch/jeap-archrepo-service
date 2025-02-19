package ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client.prometheus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
public class CloudFoundryHostProperties {

    private String host;

    @ToString.Exclude
    private String apiKey;

}
