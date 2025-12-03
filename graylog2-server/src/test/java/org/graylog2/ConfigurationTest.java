/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2;

import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import org.graylog2.configuration.ConfigurationHelper;
import org.graylog2.plugin.Tools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("deprecation")
public class ConfigurationTest {

    @TempDir
    public Path temporaryFolder;

    @Test
    public void testPasswordSecretIsTooShort() {
        final Map<String, String> validProperties = Map.of("password_secret", "too short");

        Throwable exception = assertThrows(ValidationException.class, () ->

                ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder));
        org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString("The minimum length for \"password_secret\" is 16 characters."));
    }

    @Test
    public void testPasswordSecretIsEmpty() {
        final Map<String, String> validProperties = Map.of("password_secret", "");

        Throwable exception = assertThrows(ValidationException.class, () ->

                ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder));
        org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString("Parameter password_secret should not be blank"));
    }

    @Test
    public void testPasswordSecretIsNull() {
        final HashMap<String, String> validProperties = new HashMap<>();
        validProperties.put("password_secret", null);

        Throwable exception = assertThrows(ParameterException.class, () ->

                ConfigurationHelper.initConfig(new Configuration(), (Map<String, String>) validProperties, temporaryFolder));
        org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString("Required parameter \"password_secret\" not found."));
    }

    @Test
    public void testPasswordSecretIsValid() throws ValidationException, RepositoryException {
        final Map<String, String> validProperties = Map.of("password_secret", "abcdefghijklmnopqrstuvwxyz");

        Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);

        assertThat(configuration.getPasswordSecret()).isEqualTo("abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    public void testNodeIdFilePermissions() throws IOException {
        final File nonEmptyNodeIdFile = newFile(temporaryFolder, "non-empty-node-id");
        final File emptyNodeIdFile = newFile(temporaryFolder, "empty-node-id");

        // create a node-id file and write some id
        Files.write(nonEmptyNodeIdFile.toPath(), "test-node-id".getBytes(StandardCharsets.UTF_8), WRITE, TRUNCATE_EXISTING);
        assertThat(nonEmptyNodeIdFile.length()).isGreaterThan(0);

        // parent "directory" is not a directory is not ok
        final File parentNotDirectory = new File(emptyNodeIdFile, "parent-is-file");
        assertThat(validateWithPermissions(parentNotDirectory, "rw-------")).isFalse();

        // file missing and parent directory doesn't exist is not ok
        final File directoryNotExist = newFolder(temporaryFolder, "not-readable");
        assertThat(directoryNotExist.delete()).isTrue();
        final File parentNotExist = new File(directoryNotExist, "node-id");
        assertThat(validateWithPermissions(parentNotExist, "rw-------")).isFalse();

        // file missing and parent directory not readable is not ok
        final File directoryNotReadable = newFolder(temporaryFolder, "not-readable");
        assertThat(directoryNotReadable.setReadable(false)).isTrue();
        final File parentNotReadable = new File(directoryNotReadable, "node-id");
        assertThat(validateWithPermissions(parentNotReadable, "rw-------")).isFalse();

        // file missing and parent directory not writable is not ok
        final File directoryNotWritable = newFolder(temporaryFolder, "not-writable");
        assertThat(directoryNotWritable.setWritable(false)).isTrue();
        final File parentNotWritable = new File(directoryNotWritable, "node-id");
        assertThat(validateWithPermissions(parentNotWritable, "rw-------")).isFalse();

        // file missing and parent directory readable and writable is ok
        final File parentDirectory = newFolder(temporaryFolder, "junit");
        assertThat(parentDirectory.setReadable(true)).isTrue();
        assertThat(parentDirectory.setWritable(true)).isTrue();
        final File parentOk = new File(parentDirectory, "node-id");
        assertThat(validateWithPermissions(parentOk, "rw-------")).isTrue();

        // read/write permissions should make the validation pass
        assertThat(validateWithPermissions(nonEmptyNodeIdFile, "rw-------")).isTrue();
        assertThat(validateWithPermissions(emptyNodeIdFile, "rw-------")).isTrue();

        // existing, but not writable is ok if the file is not empty
        assertThat(validateWithPermissions(nonEmptyNodeIdFile, "r--------")).isTrue();

        // existing, but not writable is not ok if the file is empty
        assertThat(validateWithPermissions(emptyNodeIdFile, "r--------")).isFalse();

        // existing, but not readable is not ok
        assertThat(validateWithPermissions(nonEmptyNodeIdFile, "-w-------")).isFalse();
    }

    @Test
    public void leaderElectionTTLTimeoutTooShort() {
        final Map<String, String> validProperties = Map.of(
                "leader_election_mode", "automatic",
                "lock_service_lock_ttl", "3s");

        assertThatThrownBy(() -> ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder))
                .isInstanceOf(ValidationException.class)
                .hasMessageStartingWith("The minimum valid \"lock_service_lock_ttl\" is");
    }

    @Test
    public void leaderElectionMinimumPollingInterval() {
        final Map<String, String> validProperties = Map.of(
                "leader_election_mode", "automatic",
                "leader_election_lock_polling_interval", "100ms");

        assertThatThrownBy(() -> ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder))
                .isInstanceOf(ValidationException.class)
                .hasMessageStartingWith("The minimum valid \"leader_election_lock_polling_interval\" is");
    }

    @Test
    public void leaderElectionTimeoutDiscrepancy() {
        final Map<String, String> validProperties = Map.of(
                "leader_election_mode", "automatic",
                "leader_election_lock_polling_interval", "2m",
                "lock_service_lock_ttl", "1m");

        assertThatThrownBy(() -> ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("needs to be greater than");
    }

    @Test
    public void isLeaderByDefault() throws Exception {
        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), Collections.emptyMap(), temporaryFolder);

        assertThat(configuration.isMaster()).isTrue();
        assertThat(configuration.isLeader()).isTrue();
    }

    @Test
    public void isMasterSetToTrue() throws Exception {
        final Map<String, String> validProperties = Map.of("is_master", "true");

        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.isMaster()).isTrue();
        assertThat(configuration.isLeader()).isTrue();
    }

    @Test
    public void isMasterSetToFalse() throws Exception {
        final Map<String, String> validProperties = Map.of("is_master", "false");

        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.isMaster()).isFalse();
        assertThat(configuration.isLeader()).isFalse();
    }

    @Test
    public void isLeaderSetToTrue() throws Exception {
        final Map<String, String> validProperties = Map.of("is_leader", "true");

        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.isMaster()).isTrue();
        assertThat(configuration.isLeader()).isTrue();
    }

    @Test
    public void isLeaderSetToFalse() throws Exception {
        final Map<String, String> validProperties = Map.of("is_leader", "false");

        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.isMaster()).isFalse();
        assertThat(configuration.isLeader()).isFalse();
    }

    @Test
    public void isMasterSetToTrueAndIsLeaderSetToTrue() throws Exception {
        final Map<String, String> validProperties = Map.of(
                "is_master", "true",
                "is_leader", "true");

        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.isMaster()).isTrue();
        assertThat(configuration.isLeader()).isTrue();
    }

    @Test
    public void isMasterSetToTrueAndIsLeaderSetToFalse() throws Exception {
        final Map<String, String> validProperties = Map.of(
                "is_master", "true",
                "is_leader", "false");

        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.isMaster()).isFalse();
        assertThat(configuration.isLeader()).isFalse();
    }

    @Test
    public void isMasterSetToFalseAndIsLeaderSetToTrue() throws Exception {
        final Map<String, String> validProperties = Map.of(
                "is_master", "false",
                "is_leader", "true");

        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.isMaster()).isTrue();
        assertThat(configuration.isLeader()).isTrue();
    }

    @Test
    public void isMasterSetToFalseAndIsLeaderSetToFalse() throws Exception {
        final Map<String, String> validProperties = Map.of(
                "is_master", "false",
                "is_leader", "false");

        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.isMaster()).isFalse();
        assertThat(configuration.isLeader()).isFalse();
    }

    @Test
    public void defaultStaleLeaderTimeout() throws Exception {
        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), Collections.emptyMap(), temporaryFolder);
        assertThat(configuration.getStaleMasterTimeout()).isEqualTo(2000);
        assertThat(configuration.getStaleLeaderTimeout()).isEqualTo(2000);
    }

    @Test
    public void staleMasterTimeoutSet() throws Exception {
        final Map<String, String> validProperties = Map.of("stale_master_timeout", "1000");
        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.getStaleMasterTimeout()).isEqualTo(1000);
        assertThat(configuration.getStaleLeaderTimeout()).isEqualTo(1000);
    }

    @Test
    public void staleLeaderTimeoutSet() throws Exception {
        final Map<String, String> validProperties = Map.of("stale_leader_timeout", "1000");
        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.getStaleMasterTimeout()).isEqualTo(1000);
        assertThat(configuration.getStaleLeaderTimeout()).isEqualTo(1000);
    }

    @Test
    public void staleLeaderTimeoutAndStaleMasterTimeoutSet() throws Exception {
        final Map<String, String> validProperties = Map.of(
                "stale_master_timeout", "1000",
                "stale_leader_timeout", "3000");
        final Configuration configuration = ConfigurationHelper.initConfig(new Configuration(), validProperties, temporaryFolder);
        assertThat(configuration.getStaleMasterTimeout()).isEqualTo(3000);
        assertThat(configuration.getStaleLeaderTimeout()).isEqualTo(3000);
    }

    @Test
    public void defaultProcessorNumbers() throws Exception {
        // number of buffer processors:
        // process, output
        final int[][] baseline = {
                {1, 1}, // 1  available processor
                {1, 1}, // 2  available processors
                {2, 1}, // 3  available processors
                {2, 1}, // 4  available processors
                {2, 1}, // 5  available processors
                {3, 2}, // 6  available processors
                {3, 2}, // 7  available processors
                {4, 2}, // 8  available processors
                {4, 2}, // 9  available processors
                {4, 2}, // 10 available processors
                {5, 2}, // 11 available processors
                {5, 3}, // 12 available processors
                {5, 3}, // 13 available processors
                {6, 3}, // 14 available processors
                {6, 3}, // 15 available processors
                {6, 3}, // 16 available processors
        };

        final int[][] actual = new int[baseline.length][2];
        for (int i = 0; i < actual.length; i++) {
            try (final var tools = Mockito.mockStatic(Tools.class)) {
                tools.when(Tools::availableProcessors).thenReturn(i + 1);
                final Configuration config = ConfigurationHelper.initConfig(new Configuration(), Collections.emptyMap(), temporaryFolder);
                actual[i][0] = config.getProcessBufferProcessors();
                actual[i][1] = config.getOutputBufferProcessors();
            }
        }
        assertThat(actual).isEqualTo(baseline);
    }

    /**
     * Run the NodeIDFileValidator on a file with the given permissions.
     *
     * @param nodeIdFile  the path to the node id file, can be missing
     * @param permissions the posix permission to set the file to, if it exists, before running the validator
     * @return true if the validation was successful, false otherwise
     * @throws IOException if any file related problem occurred
     */
    private static boolean validateWithPermissions(File nodeIdFile, String permissions) throws IOException {
        try {
            final Configuration.NodeIdFileValidator validator = new Configuration.NodeIdFileValidator();
            if (nodeIdFile.exists()) {
                Files.setPosixFilePermissions(nodeIdFile.toPath(), PosixFilePermissions.fromString(permissions));
            }
            validator.validate("node-id", nodeIdFile.toString());
        } catch (ValidationException ve) {
            return false;
        }
        return true;
    }

    private static File newFile(Path parent, String child) throws IOException {
        return Files.createFile(parent.resolve(child)).toFile();
    }

    private static File newFolder(Path root, String... subDirs) throws IOException {
        final Path path = Path.of(root.toString(), subDirs);
        return Files.createDirectories(path).toFile();
    }
}
