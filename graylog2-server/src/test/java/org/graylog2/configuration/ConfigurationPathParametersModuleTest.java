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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.collect.ImmutableMap;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isA;

public class ConfigurationPathParametersModuleTest {
    private static final Path ABSOLUTE_PATH = Paths.get("").toAbsolutePath();

    private static class Config {
        @Parameter("bin_path")
        public Path binPath = null;

        @Parameter("data_dir")
        public Path dataDir = null;

        @Parameter("message_journal_dir1")
        @GraylogDataDir
        public Path messageJournalDir1 = null;

        @Parameter("message_journal_dir2")
        @GraylogDataDir
        public Path messageJournalDir2 = null;
    }

    private static class MyClass {
        final Path binPath;
        final Path journalDir1;
        final Path journalDir2;

        @Inject
        public MyClass(@GraylogBinPath Path binPath,
                       @GraylogDataDir("message_journal_dir1") Path journalDir1,
                       @GraylogDataDir("message_journal_dir2") Path journalDir2) {
            this.binPath = binPath;
            this.journalDir1 = journalDir1;
            this.journalDir2 = journalDir2;
        }
    }

    private MyClass getInstanceWithConfig(Map<String, String> configMap) throws Exception {
        final InMemoryRepository repository = new InMemoryRepository(configMap);
        JadConfig jadConfig = new JadConfig(repository, new Config());
        jadConfig.process();

        return Guice.createInjector(new ConfigurationPathParametersModule(jadConfig.getConfigurationBeans())).getInstance(MyClass.class);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testBinPath() throws Exception {
        final MyClass instance = getInstanceWithConfig(ImmutableMap.of(
                "bin_path", "bin",
                "data_dir", "data",
                "message_journal_dir1", "journal",
                "message_journal_dir2", "journal"
        ));

        assertThat(instance.binPath)
                .isAbsolute()
                .isEqualTo(ABSOLUTE_PATH.resolve("bin"));
    }

    @Test
    public void testBinPathWithoutValue() throws Exception {
        expectedException.expect(CreationException.class);
        expectedException.expectCause(isA(IllegalStateException.class));

        getInstanceWithConfig(ImmutableMap.of(
                "data_dir", "data",
                "message_journal_dir1", "journal",
                "message_journal_dir2", "journal"
        ));
    }

    @Test
    public void testDataDir() throws Exception {
        final MyClass instance = getInstanceWithConfig(ImmutableMap.of(
                "bin_path", "bin",
                "data_dir", "data",
                "message_journal_dir1", "jj",
                "message_journal_dir2", "/tmp/journal"
        ));

        // If config value is a relative path, data_dir is used as base
        assertThat(instance.journalDir1)
                .isAbsolute()
                .isEqualTo(ABSOLUTE_PATH.resolve("data/jj"));

        // If config value is an absolute path, that one is used
        assertThat(instance.journalDir2)
                .isAbsolute()
                .isEqualTo(Paths.get("/tmp/journal"));
    }

    @Test
    public void testDataDirWithoutValue() throws Exception {
        expectedException.expect(CreationException.class);
        expectedException.expectCause(isA(IllegalStateException.class));

        getInstanceWithConfig(ImmutableMap.of(
                "bin_path", "bin",
                "message_journal_dir1", "journal",
                "message_journal_dir2", "journal"
        ));
    }
}