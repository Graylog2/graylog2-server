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
package org.graylog.testing.graylognode;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.graylog.testing.graylognode.ExecutableFileUtil.makeSureExecutableIsFound;

public class MavenPackager {
    private static final Logger LOG = LoggerFactory.getLogger(MavenPackager.class);
    private static final String MVN_COMMAND = "mvn package -DskipTests -Dskip.web.build -Dforbiddenapis.skip=true -Dmaven.javadoc.skip=true";

    static void packageJarIfNecessary(Path projectDir) {
        if (isRunFromMaven()) {
            LOG.info("Running from Maven. Assuming jars are current.");
        } else {
            LOG.info("Running from outside Maven. Packaging server jar now...");
            makeSureExecutableIsFound("mvn");
            packageJar(projectDir);
        }
    }

    private static boolean isRunFromMaven() {
        // surefire-related properties should only be present when the tests are started from surefire, i.e. maven
        return System.getProperty("surefire.test.class.path") != null;
    }

    static void packageJar(Path pomDir) {
        Process p = startProcess(pomDir);

        Stopwatch sw = Stopwatch.createStarted();

        int exitCode = waitForExit(p);

        sw.stop();
        LOG.info("Finished packaging after {} seconds", sw.elapsed(TimeUnit.SECONDS));

        ensureZeroExitCode(p, exitCode);
    }

    private static int waitForExit(Process p) {
        try {
            return p.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("Process for mvn package was interrupted", e);
        }
    }

    private static Process startProcess(Path pomDir) {
        try {
            return new ProcessBuilder().command("sh", "-c", MVN_COMMAND).directory(pomDir.toFile()).inheritIO().start();
        } catch (IOException e) {
            String msg = String.format(Locale.US, "Failed to start maven process with command [%s].", MVN_COMMAND);
            throw new RuntimeException(msg, e);
        }
    }

    private static void ensureZeroExitCode(Process p, int exitCode) {
        if (exitCode == 0) {
            return;
        }
        if (exitCode == 127) {
            String msg = String.format(Locale.US, "/bin/sh couldn't find Maven on your PATH when attempting to run [%s]", MVN_COMMAND);
            throw new RuntimeException(msg);
        }

        printOutputFrom(p);

        String msg = String.format(Locale.US, "Maven exited with %s after running [%s]. ", exitCode, MVN_COMMAND);
        throw new RuntimeException(msg);

    }

    private static void printOutputFrom(Process p) {
        new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset())).lines()
                .forEach(System.out::println);
    }
}
