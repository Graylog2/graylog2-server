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
package org.graylog2.shared.plugins;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.testng.Assert.assertTrue;

public class PluginComparatorTest {
    private PluginLoader.PluginComparator comparator = new PluginLoader.PluginComparator();

    @DataProvider(name = "test", parallel = true)
    public static Object[][] provideData() {
        return new Object[][]{
                {new TestPlugin("u", "n", new Version(1, 0, 0)), new TestPlugin("u", "n", new Version(1, 0, 0)), 0},
                {new TestPlugin("u1", "n", new Version(1, 0, 0)), new TestPlugin("u2", "n", new Version(1, 0, 0)), -1},
                {new TestPlugin("u", "n1", new Version(1, 0, 0)), new TestPlugin("u", "n2", new Version(1, 0, 0)), -1},
                {new TestPlugin("u2", "n1", new Version(1, 0, 0)), new TestPlugin("u1", "n2", new Version(1, 0, 0)), 1},
                {new TestPlugin("u", "n", new Version(1, 0, 0, "beta.1")), new TestPlugin("u", "n", new Version(1, 0, 0)), -1},
                {new TestPlugin("u", "n", new Version(1, 0, 0, "beta.1")), new TestPlugin("u", "n", new Version(1, 0, 0, "alpha.5")), 1},
                {new TestPlugin("u", "n", new Version(1, 0, 1)), new TestPlugin("u", "n", new Version(1, 0, 0)), 1},
                {new TestPlugin("u", "n", new Version(1, 0, 0)), new TestPlugin("u", "n", new Version(1, 0, 1)), -1},
                {new TestPlugin("u", "n", new Version(2, 0, 0)), new TestPlugin("u", "n", new Version(1, 0, 0)), 1},
                {new TestPlugin("u", "n", new Version(1, 1, 0)), new TestPlugin("u", "n", new Version(1, 0, 0)), 1},
                {new TestPlugin("u", "n", new Version(1, 0, 1)), new TestPlugin("u", "n", new Version(1, 0, 0)), 1}
        };
    }

    @Test(dataProvider = "test")
    public void testCompare(Plugin p1, Plugin p2, int result) throws Exception {
        assertTrue(comparator.compare(p1, p2) == result);
    }

    public static class TestPlugin implements Plugin {
        private final String uniqueId;
        private final String name;
        private final Version version;

        public TestPlugin(String uniqueId, String name, Version version) {
            this.uniqueId = uniqueId;
            this.name = name;
            this.version = version;
        }

        @Override
        public PluginMetaData metadata() {
            return new PluginMetaData() {
                @Override
                public String getUniqueId() {
                    return uniqueId;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getAuthor() {
                    return null;
                }

                @Override
                public URI getURL() {
                    return null;
                }

                @Override
                public Version getVersion() {
                    return version;
                }

                @Override
                public String getDescription() {
                    return null;
                }

                @Override
                public Version getRequiredVersion() {
                    return null;
                }

                @Override
                public Set<ServerStatus.Capability> getRequiredCapabilities() {
                    return null;
                }
            };
        }

        @Override
        public Collection<PluginModule> modules() {
            return Collections.emptySet();
        }

        @Override
        public String toString() {
            return uniqueId + " " + name + " " + version;
        }
    }
}