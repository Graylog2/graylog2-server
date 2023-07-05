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
package org.graylog.datanode.configuration.verification;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.graylog.datanode.configuration.verification.ConfigSectionCompleteness.COMPLETE;
import static org.graylog.datanode.configuration.verification.ConfigSectionCompleteness.INCOMPLETE;
import static org.graylog.datanode.configuration.verification.ConfigSectionCompleteness.MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigSectionCompletenessVerifierTest {

    private static Path existingFile;

    @BeforeAll
    static void setUp(@TempDir Path tmpDir) throws IOException {
        existingFile = tmpDir.resolve("lalala.txt");
        Files.createFile(existingFile);
    }

    @Test
    void testReturnsOkOnNullRequirements() {
        assertEquals(COMPLETE, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(null));
    }

    @Test
    void testReturnsOkOnEmptyRequirements() {
        assertEquals(COMPLETE, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(new ConfigSectionRequirements(null, null)));
        assertEquals(COMPLETE, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(new ConfigSectionRequirements(List.of(), List.of())));
    }

    @Test
    void testReturnsOkIfAllRequirementsMet() {
        assertEquals(COMPLETE, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(new ConfigSectionRequirements(
                List.of(new ConfigProperty("importantProperty", "I am present"),
                        new ConfigProperty("whatever", "Mee too!")),
                List.of(existingFile)
        )));
    }

    @Test
    void testReturnsIncompleteIfSomePropertiesMissing() {
        assertEquals(INCOMPLETE, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(new ConfigSectionRequirements(
                List.of(new ConfigProperty("importantProperty", "I am present"),
                        new ConfigProperty("whatever", "Mee too!"),
                        new ConfigProperty("nvmd", "")
                ),
                List.of()
        )));
    }

    @Test
    void testReturnsIncompleteIfSomePropertiesNull() {
        assertEquals(INCOMPLETE, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(new ConfigSectionRequirements(
                List.of(new ConfigProperty("importantProperty", "I am present"),
                        new ConfigProperty("whatever", "Mee too!"),
                        new ConfigProperty("nvmd", null)
                ),
                List.of()
        )));
    }

    @Test
    void testReturnsIncompleteIfNullFilePathPresent() {
        assertEquals(INCOMPLETE, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(new ConfigSectionRequirements(
                List.of(new ConfigProperty("importantProperty", "I am present"),
                        new ConfigProperty("whatever", "Mee too!")),
                Arrays.asList(existingFile, null)
        )));
    }

    @Test
    void testReturnsIncompleteIfNonExistingFilePathPresent() {
        assertEquals(INCOMPLETE, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(new ConfigSectionRequirements(
                List.of(new ConfigProperty("importantProperty", "I am present"),
                        new ConfigProperty("whatever", "Mee too!")),
                List.of(existingFile, Path.of("I_am_not_at_home.txt"))
        )));
    }

    @Test
    void testReturnsMissingIfAllPropertiesRequirementsAreNotMet() {
        assertEquals(MISSING, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(new ConfigSectionRequirements(
                Arrays.asList(new ConfigProperty("importantProperty", ""),
                        new ConfigProperty("whatever", ""),
                        new ConfigProperty("x", null)),
                Arrays.asList(null, null, Path.of("I_am_not_at_home.txt"))
        )));
        assertEquals(MISSING, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(new ConfigSectionRequirements(
                List.of(),
                List.of(Path.of("I_am_not_at_home.txt"))
        )));
        assertEquals(MISSING, new ConfigSectionCompletenessVerifier().verifyConfigSectionCompleteness(new ConfigSectionRequirements(
                List.of(new ConfigProperty("x", "")),
                List.of()
        )));
    }


}
