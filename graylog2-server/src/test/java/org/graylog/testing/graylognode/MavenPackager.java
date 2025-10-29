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
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.graylog.testing.graylognode.NodeContainerConfig.flagFromEnvVar;

public class MavenPackager {
    private static final Logger LOG = LoggerFactory.getLogger(MavenPackager.class);
    private static final String MVN_COMMAND = "./mvnw -V package -DskipTests -Dforbiddenapis.skip=true -Dmaven.javadoc.skip=true -Dcyclonedx.skip -Dskip.artifact.assembly ";
    private static final String SKIP_FLAG = "GRAYLOG_IT_SKIP_PACKAGING";

    private static boolean jarHasBeenPackagedInThisRun = false;

    private static String getMavenCommand() {
        return MVN_COMMAND;
    }

    public static synchronized void packageJarIfNecessary(final MavenProjectDirProvider mavenProjectDirProvider) {
        if (flagFromEnvVar(SKIP_FLAG)) {
            LOG.info("Skipping packaging - {} is set", SKIP_FLAG);
            return;
        }
        if (isRunFromMaven()) {
            LOG.debug("Running from Maven - assuming JAR files have been built.");
        } else if (jarHasBeenPackagedInThisRun) {
            LOG.debug("Assuming JAR files have been built.");
        } else {
            LOG.info("Running from outside Maven - packaging JAR files now...");
            packageJar(mavenProjectDirProvider);
        }
    }

    public static boolean isRunFromMaven() {
        // surefire-related properties should only be present when the tests are started from surefire, i.e. maven
        return System.getProperty("surefire.test.class.path") != null;
    }

    public static void packageJar(final MavenProjectDirProvider mavenProjectDirProvider) {
        Path pomDir = mavenProjectDirProvider.getProjectDir();

        Process p = startProcess(pomDir);

        Stopwatch sw = Stopwatch.createStarted();

        int exitCode = waitForExit(p);

        sw.stop();
        LOG.info("Finished packaging after {} seconds", sw.elapsed(TimeUnit.SECONDS));
        jarHasBeenPackagedInThisRun = true;

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
            return new ProcessBuilder().command("sh", "-c", getMavenCommand()).directory(pomDir.toFile()).inheritIO().start();
        } catch (IOException e) {
            String msg = String.format(Locale.US, "Failed to start maven process with command [%s].", getMavenCommand());
            throw new RuntimeException(msg, e);
        }
    }

    private static void ensureZeroExitCode(Process p, int exitCode) {
        if (exitCode == 0) {
            return;
        }
        if (exitCode == 127) {
            String msg = String.format(Locale.US, "/bin/sh couldn't find Maven on your PATH when attempting to run [%s]", getMavenCommand());
            throw new RuntimeException(msg);
        }

        printOutputFrom(p);

        String msg = String.format(Locale.US, "Maven exited with %s after running [%s]. ", exitCode, getMavenCommand());
        throw new RuntimeException(msg);

    }

    private static void printOutputFrom(Process p) {
        new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset())).lines()
                .forEach(System.out::println);
    }
}
