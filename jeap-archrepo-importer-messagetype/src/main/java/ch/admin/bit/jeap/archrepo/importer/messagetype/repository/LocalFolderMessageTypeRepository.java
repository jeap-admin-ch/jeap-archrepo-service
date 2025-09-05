package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

/**
 * A repository implementation for local Git repositories.
 * This type is only used in local integration tests
 */
public class LocalFolderMessageTypeRepository extends MessageTypeRepository {

    LocalFolderMessageTypeRepository(String gitUri) {
        super(gitUri);
    }

    @Override
    protected String processBaseUri(String gitUri) {
        return "";
    }

}
