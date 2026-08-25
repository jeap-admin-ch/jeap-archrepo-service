package ch.admin.bit.jeap.archrepo.persistence;

import java.time.ZonedDateTime;

/**
 * One entry of an artifact index: everything the docs API needs to tell a consumer whether it has to fetch the
 * artifact, without reading the artifact itself.
 */
public interface ArtifactIndexEntry {

    String getSystem();

    String getComponent();

    /**
     * Version of the artifact - the spec version, or the database schema version.
     */
    String getVersion();

    /**
     * SHA-256 of the stored bytes, null for artifacts stored before the content hash column existed.
     */
    String getContentHash();

    ZonedDateTime getLastModifiedAt();
}
