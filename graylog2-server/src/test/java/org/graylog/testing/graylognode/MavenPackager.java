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
import java.util.concurrent.TimeUnit;

public class MavenPackager {
    private static final Logger LOG = LoggerFactory.getLogger(MavenPackager.class);
    private static final String MVN_COMMAND = "mvn package -DskipTests -Dskip.web.build";

    static void packageJar(String pomDir) {

        Process p = startProcess(pomDir);

        Stopwatch sw = Stopwatch.createStarted();

        int exitCode = waitForExit(p);

        sw.stop();
        LOG.info("Finished packaging after " + sw.elapsed(TimeUnit.SECONDS) + " seconds");

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
            throw new RuntimeException("Failed to start process for mvn package", e);
        }
    }

    private static void ensureZeroExitCode(Process p, int exitCode) {
        if (exitCode > 0) {
            new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset())).lines()
                    .forEach(System.out::println);

            throw new RuntimeException("mvn package exited with " + exitCode);
        }
    }
}
