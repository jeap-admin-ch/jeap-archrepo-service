package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import org.eclipse.jgit.transport.CredentialsProvider;

/**
 * An abstract implementation of the {@link CredentialsProvider} that provides
 * a mechanism to determine if the provider supports a given URL.
 * Subclasses should override the {@code supports} method to implement
 * specific logic for URL-based credential support.
 */
public abstract class UrlBasedCredentialsProvider extends CredentialsProvider {

    /**
     * Determines whether this credentials provider supports the given URL.
     *
     * @param url the URL to check for support
     * @return {@code false} by default. Subclasses should override this method
     *         to provide specific support logic.
     */
    public boolean supports(String url) {
        return false;
    }

}
