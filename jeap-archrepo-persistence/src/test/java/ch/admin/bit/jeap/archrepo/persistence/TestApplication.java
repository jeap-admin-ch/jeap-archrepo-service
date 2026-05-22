package ch.admin.bit.jeap.archrepo.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"ch.admin.bit.jeap.archrepo.metamodel", "ch.admin.bit.jeap.archrepo.persistence"})
public class TestApplication {}