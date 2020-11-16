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

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Map<String, String> validProperties;

    @Before
    public void setUp() throws Exception {
        validProperties = new HashMap<>();

        // Required properties
        validProperties.put("password_secret", "ipNUnWxmBLCxTEzXcyamrdy0Q3G7HxdKsAvyg30R9SCof0JydiZFiA3dLSkRsbLF");
        // SHA-256 of "admin"
        validProperties.put("root_password_sha2", "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918");
    }

    @Test
    public void testPasswordSecretIsTooShort() throws ValidationException, RepositoryException {
        validProperties.put("password_secret", "too short");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("The minimum length for \"password_secret\" is 16 characters.");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testPasswordSecretIsEmpty() throws ValidationException, RepositoryException {
        validProperties.put("password_secret", "");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Parameter password_secret should not be blank");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testPasswordSecretIsNull() throws ValidationException, RepositoryException {
        validProperties.put("password_secret", null);

        expectedException.expect(ParameterException.class);
        expectedException.expectMessage("Required parameter \"password_secret\" not found.");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();
    }

    @Test
    public void testPasswordSecretIsValid() throws ValidationException, RepositoryException {
        validProperties.put("password_secret", "abcdefghijklmnopqrstuvwxyz");

        Configuration configuration = new Configuration();
        new JadConfig(new InMemoryRepository(validProperties), configuration).process();

        assertThat(configuration.getPasswordSecret()).isEqualTo("abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    public void testNodeIdFilePermissions() throws IOException {
        final File nonEmptyNodeIdFile = temporaryFolder.newFile("non-empty-node-id");
        final File emptyNodeIdFile = temporaryFolder.newFile("empty-node-id");

        // create a node-id file and write some id
        Files.write(nonEmptyNodeIdFile.toPath(), "test-node-id".getBytes(StandardCharsets.UTF_8), WRITE, TRUNCATE_EXISTING);
        assertThat(nonEmptyNodeIdFile.length()).isGreaterThan(0);

        // parent "directory" is not a directory is not ok
        final File parentNotDirectory = new File(emptyNodeIdFile, "parent-is-file");
        assertThat(validateWithPermissions(parentNotDirectory, "rw-------")).isFalse();

        // file missing and parent directory doesn't exist is not ok
        final File directoryNotExist = temporaryFolder.newFolder("not-readable");
        assertThat(directoryNotExist.delete()).isTrue();
        final File parentNotExist = new File(directoryNotExist, "node-id");
        assertThat(validateWithPermissions(parentNotExist, "rw-------")).isFalse();

        // file missing and parent directory not readable is not ok
        final File directoryNotReadable = temporaryFolder.newFolder("not-readable");
        assertThat(directoryNotReadable.setReadable(false)).isTrue();
        final File parentNotReadable = new File(directoryNotReadable, "node-id");
        assertThat(validateWithPermissions(parentNotReadable, "rw-------")).isFalse();

        // file missing and parent directory not writable is not ok
        final File directoryNotWritable = temporaryFolder.newFolder("not-writable");
        assertThat(directoryNotWritable.setWritable(false)).isTrue();
        final File parentNotWritable = new File(directoryNotWritable, "node-id");
        assertThat(validateWithPermissions(parentNotWritable, "rw-------")).isFalse();

        // file missing and parent directory readable and writable is ok
        final File parentDirectory = temporaryFolder.newFolder();
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

    /**
     * Run the NodeIDFileValidator on a file with the given permissions.
     * @param nodeIdFile the path to the node id file, can be missing
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
}
