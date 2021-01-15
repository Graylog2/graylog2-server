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
package org.graylog2.shared.plugins;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class PluginComparatorTest {
    private PluginLoader.PluginComparator comparator = new PluginLoader.PluginComparator();

    @Parameterized.Parameters
    public static Object[][] provideData() {
        return new Object[][]{
                {new TestPlugin("u", "n", Version.from(1, 0, 0)), new TestPlugin("u", "n", Version.from(1, 0, 0)), 0},
                {new TestPlugin("u1", "n", Version.from(1, 0, 0)), new TestPlugin("u2", "n", Version.from(1, 0, 0)), -1},
                {new TestPlugin("u", "n1", Version.from(1, 0, 0)), new TestPlugin("u", "n2", Version.from(1, 0, 0)), -1},
                {new TestPlugin("u2", "n1", Version.from(1, 0, 0)), new TestPlugin("u1", "n2", Version.from(1, 0, 0)), 1},
                {new TestPlugin("u", "n", Version.from(1, 0, 0, "beta.1")), new TestPlugin("u", "n", Version.from(1, 0, 0)), -1},
                {new TestPlugin("u", "n", Version.from(1, 0, 0, "beta.1")), new TestPlugin("u", "n", Version.from(1, 0, 0, "alpha.5")), 1},
                {new TestPlugin("u", "n", Version.from(1, 0, 1)), new TestPlugin("u", "n", Version.from(1, 0, 0)), 1},
                {new TestPlugin("u", "n", Version.from(1, 0, 0)), new TestPlugin("u", "n", Version.from(1, 0, 1)), -1},
                {new TestPlugin("u", "n", Version.from(2, 0, 0)), new TestPlugin("u", "n", Version.from(1, 0, 0)), 1},
                {new TestPlugin("u", "n", Version.from(1, 1, 0)), new TestPlugin("u", "n", Version.from(1, 0, 0)), 1},
                {new TestPlugin("u", "n", Version.from(1, 0, 1)), new TestPlugin("u", "n", Version.from(1, 0, 0)), 1}
        };
    }

    private Plugin first;
    private Plugin second;
    private int comparisonResult;

    public PluginComparatorTest(Plugin first, Plugin second, int comparisonResult) {
        this.first = first;
        this.second = second;
        this.comparisonResult = comparisonResult;
    }

    @Test
    public void testCompare() throws Exception {
        assertTrue(comparator.compare(first, second) == comparisonResult);
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
