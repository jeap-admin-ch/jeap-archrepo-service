package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import java.io.File;

class MessageTypeRepoException extends RuntimeException {
    private MessageTypeRepoException(String message) {
        super(message);
    }

    private MessageTypeRepoException(String message, Throwable cause) {
        super(message, cause);
    }

    static MessageTypeRepoException cloneFailed(String gitUri, Throwable cause) {
        return new MessageTypeRepoException("Cannot clone message type repository %s" + gitUri, cause);
    }

    static MessageTypeRepoException missingDescriptor(String path) {
        String message = "Message type directory %s does not contain a JSON descriptor".formatted(path);
        return new MessageTypeRepoException(message);
    }

    static MessageTypeRepoException missingSchema(String schemaName, File globalDirSchemaFile, File commonDirSchemaFile, File schemaFile, String messageTypeName) {
        String message = "Cannot find avro schema %s for message type %s at %s or %s or %s".formatted(schemaName, messageTypeName, globalDirSchemaFile, commonDirSchemaFile, schemaFile);
        return new MessageTypeRepoException(message);
    }

    static MessageTypeRepoException descriptorParsingFailed(String path, Throwable cause) {
        String message = "Cannot parse message type descriptor " + path;
        return new MessageTypeRepoException(message, cause);
    }
}
