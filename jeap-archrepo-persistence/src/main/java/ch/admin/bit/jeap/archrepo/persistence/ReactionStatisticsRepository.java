package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.reaction.ReactionStatistics;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReactionStatisticsRepository extends JpaRepository<ReactionStatistics, UUID> {

    List<ReactionStatistics> findByComponent(SystemComponent component);

    @Query("select rs.component.name as component, max(rs.modifiedAt) as lastModifiedAt from ReactionStatistics rs group by rs.component.name order by rs.component.name")
    List<ReactionStatisticsLastModifiedAt> getMaxLastModifiedAtList();

}
