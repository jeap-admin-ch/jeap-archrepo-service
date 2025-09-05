package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@SuppressWarnings("findbugs:NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
@Slf4j
public abstract class MessageTypeRepository implements Closeable {
    private static final String COMMON = "_common";

    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper objectMapper;
    private final String gitUri;
    private File gitRepoPath;
    private final String repoLinkHttpBaseUri;
    @Setter
    private CredentialsProvider credentialsProvider;

    protected MessageTypeRepository(String gitUri) {
        this.gitUri = gitUri;
        this.repoLinkHttpBaseUri = processBaseUri(gitUri);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected String processBaseUri(String gitUri) {
        return gitUri;
    }

    @Override
    public void close() {
        log.info("Deleting {}", gitRepoPath);
        try {
            FileUtils.forceDelete(gitRepoPath);
        } catch (IOException e) {
            log.error("Failed to delete cloned git repo", e);
        }
    }

    public List<EventDescriptor> getAllEventDescriptors() {
        return getSystemDirs()
                .flatMap(systemDir -> getMessageTypeDirs(systemDir, EventDescriptor.SUBDIR))
                .map(this::getDescriptorFile)
                .map(descriptorFile -> readDescriptor(descriptorFile, EventDescriptor.class))
                .toList();
    }

    public List<CommandDescriptor> getAllCommandDescriptors() {
        return getSystemDirs()
                .flatMap(systemDir -> getMessageTypeDirs(systemDir, CommandDescriptor.SUBDIR))
                .map(this::getDescriptorFile)
                .map(descriptorFile -> readDescriptor(descriptorFile, CommandDescriptor.class))
                .toList();
    }

    public File cloneGitRepo() {
        try {
            this.gitRepoPath = Files.createTempDirectory("messageTypeRepo").toFile();
            this.gitRepoPath.deleteOnExit();
            Git.cloneRepository()
                    .setURI(gitUri)
                    .setDirectory(this.gitRepoPath)
                    .setCredentialsProvider(credentialsProvider)
                    .call();
            return this.gitRepoPath;
        } catch (IOException | GitAPIException e) {
            throw MessageTypeRepoException.cloneFailed(gitUri, e);
        }
    }

    private Stream<File> getSystemDirs() {
        File descriptorDir = new File(gitRepoPath, "descriptor");
        return Arrays.stream(Objects.requireNonNull(descriptorDir.list()))
                .filter(name -> !name.equals(COMMON))
                .map(name -> new File(descriptorDir, name));
    }

    private static Stream<File> getMessageTypeDirs(File systemDir, String typeSubdir) {
        File typeDir = new File(systemDir, typeSubdir);
        if (!typeDir.isDirectory()) {
            return Stream.empty();
        }
        return Arrays.stream(Objects.requireNonNull(typeDir.list()))
                .map(eventDirName -> new File(typeDir, eventDirName));
    }

    private File getDescriptorFile(File messageTypeDir) {
        String[] jsonFileNames = messageTypeDir.list((f, n) -> FilenameUtils.getExtension(n).equals("json"));
        if (jsonFileNames == null || jsonFileNames.length == 0) {
            throw MessageTypeRepoException.missingDescriptor(messageTypeDir.getAbsolutePath());
        }
        return new File(messageTypeDir, jsonFileNames[0]);
    }

    private <T> T readDescriptor(File descriptorFile, Class<T> descriptorType) {
        try {
            JsonParser jsonParser = jsonFactory.createParser(descriptorFile);
            return objectMapper.readValue(jsonParser, descriptorType);
        } catch (IOException e) {
            throw MessageTypeRepoException.descriptorParsingFailed(descriptorFile.getAbsolutePath(), e);
        }
    }

    public String getDescriptorUrl(MessageTypeDescriptor descriptor) {
        return String.format("%sbrowse/descriptor/%s/%s/%s/%s.json",
                repoLinkHttpBaseUri,
                descriptor.getDefiningSystem().toLowerCase(),
                descriptor.getMessageTypeSubdir(),
                descriptor.getMessageTypeName().toLowerCase(),
                descriptor.getMessageTypeName());
    }

    public String getSchemaUrl(MessageTypeDescriptor descriptor, String schemaName) {
        File descriptorDir = new File(gitRepoPath, "descriptor");
        File commonDir = new File(descriptorDir, COMMON);
        File globalDirSchemaFile = new File(commonDir, schemaName);
        if (globalDirSchemaFile.exists()) {
            return String.format("%sbrowse/descriptor/_common/%s",
                    repoLinkHttpBaseUri,
                    schemaName);
        }
        File systemDir = new File(descriptorDir, descriptor.getDefiningSystem().toLowerCase());
        File commonSystemDir = new File(systemDir, COMMON);
        File commonDirSchemaFile = new File(commonSystemDir, schemaName);
        if (commonDirSchemaFile.exists()) {
            return String.format("%sbrowse/descriptor/%s/_common/%s",
                    repoLinkHttpBaseUri,
                    descriptor.getDefiningSystem().toLowerCase(),
                    schemaName);
        }
        File messageTypeSubdir = new File(systemDir, descriptor.getMessageTypeSubdir());
        File typeDir = new File(messageTypeSubdir, descriptor.getMessageTypeName().toLowerCase());
        String subdir = descriptor.getMessageTypeSubdir();
        File schemaFile = new File(typeDir, schemaName);
        if (schemaFile.exists()) {
            return String.format("%sbrowse/descriptor/%s/%s/%s/%s",
                    repoLinkHttpBaseUri,
                    descriptor.getDefiningSystem().toLowerCase(),
                    subdir,
                    descriptor.getMessageTypeName().toLowerCase(),
                    schemaName);
        }
        throw MessageTypeRepoException.missingSchema(schemaName, globalDirSchemaFile, commonDirSchemaFile, schemaFile, descriptor.getMessageTypeName());
    }

    public File getSchemaFile(MessageTypeDescriptor descriptor, String schemaName) {
        File descriptorDir = new File(gitRepoPath, "descriptor");
        File commonDir = new File(descriptorDir, COMMON);
        File globalDirSchemaFile = new File(commonDir, schemaName);
        if (globalDirSchemaFile.exists()) {
            return globalDirSchemaFile;
        }
        File systemDir = new File(descriptorDir, descriptor.getDefiningSystem().toLowerCase());
        File commonSystemDir = new File(systemDir, COMMON);
        File commonDirSchemaFile = new File(commonSystemDir, schemaName);
        if (commonDirSchemaFile.exists()) {
            return commonDirSchemaFile;
        }
        File messageTypeSubdir = new File(systemDir, descriptor.getMessageTypeSubdir());
        File typeDir = new File(messageTypeSubdir, descriptor.getMessageTypeName().toLowerCase());
        File schemaFile = new File(typeDir, schemaName);
        if (schemaFile.exists()) {
            return schemaFile;
        }
        throw MessageTypeRepoException.missingSchema(schemaName, globalDirSchemaFile, commonDirSchemaFile, schemaFile, descriptor.getMessageTypeName());
    }

}
