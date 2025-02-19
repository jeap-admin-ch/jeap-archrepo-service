package ch.admin.bit.jeap.archrepo.importer.messagetype;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;

public abstract class CreateLocalGitRepoBaseTest {

    protected static String repoUrl;

    @BeforeAll
    static void beforeAll() throws Exception {
        File repoDir = new File("target/test-git-repo");
        FileUtils.copyDirectory(new File("src/test/resources/test-message-type-registry"), repoDir);
        Git newRepo = Git.init()
                .setDirectory(repoDir)
                .call();
        newRepo.add()
                .addFilepattern(".")
                .call();
        newRepo.commit()
                .setMessage("Initial revision")
                .call();

        newRepo.close();
        repoUrl = "file://" + repoDir.getAbsolutePath();
    }
}
