/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.testing.graylognode;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MavenPackager {
    private static final Logger LOG = LoggerFactory.getLogger(MavenPackager.class);
    private static final String MVN_COMMAND = "mvn package -DskipTests -Dskip.web.build -Dforbiddenapis.skip=true -Dmaven.javadoc.skip=true";

    static void packageJarIfNecessary(String projectDir) {
        if (isRunFromMaven()) {
            LOG.info("Running from Maven. Assuming jars are current.");
        } else {
            LOG.info("Running from outside Maven. Packaging server jar now...");
            makeSureMavenExecutableIsFound();
            packageJar(projectDir);
        }
    }

    private static boolean isRunFromMaven() {
        // surefire-related properties should only be present when the tests are started from surefire, i.e. maven
        return System.getProperty("surefire.test.class.path") != null;
    }

    private static void makeSureMavenExecutableIsFound() {
        String mavenExecutable = "mvn";
        String path = System.getenv("PATH");
        for (String dirname : path.split(File.pathSeparator)) {
            File file = new File(dirname, mavenExecutable);
            if (file.isFile() && file.canExecute()) {
                LOG.info("Found Maven executable at " + file.getAbsolutePath());
                return;
            }
        }
        throw new RuntimeException(String.format(Locale.US, "Could not find Maven executable in PATH [%s]", path));
    }

    static void packageJar(String pomDir) {
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

    private static Process startProcess(String pomDir) {
        try {
            return new ProcessBuilder().command("sh", "-c", MVN_COMMAND).directory(new File(pomDir)).start();
        } catch (IOException e) {
            String msg = String.format(Locale.US, "Failed to start maven process with command [%s].", MVN_COMMAND);
            throw new RuntimeException(msg, e);
        }
    }

    private static void ensureZeroExitCode(Process p, int exitCode) {
        if (exitCode > 0) {
            new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset())).lines()
                    .forEach(System.out::println);

        String msg = String.format(Locale.US, "Maven exited with %s after running [%s]. ", exitCode, MVN_COMMAND);
        throw new RuntimeException(msg);

    }
}
