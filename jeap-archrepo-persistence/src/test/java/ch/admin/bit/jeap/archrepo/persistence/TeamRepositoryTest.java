package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void findByName() {
        teamRepository.save(new Team("junit", "test", "test", "test"));
        Optional<Team> team = teamRepository.findByName("junit");
        assertThat(team).isPresent();
    }


}

