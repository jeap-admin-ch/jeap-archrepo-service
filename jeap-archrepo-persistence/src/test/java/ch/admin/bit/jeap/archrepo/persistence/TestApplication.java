package ch.admin.bit.jeap.archrepo.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "ch.admin.bit.jeap.archrepo.metamodel")
public class TestApplication {}