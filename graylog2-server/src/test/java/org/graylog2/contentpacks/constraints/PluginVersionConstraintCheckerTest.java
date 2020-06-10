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
package org.graylog2.contentpacks.constraints;

import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginVersionConstraintCheckerTest {
    @Test
    public void checkConstraints() {
        final TestPluginMetaData pluginMetaData = new TestPluginMetaData();
        final PluginVersionConstraintChecker constraintChecker = new PluginVersionConstraintChecker(Collections.singleton(pluginMetaData));

        final GraylogVersionConstraint graylogVersionConstraint = GraylogVersionConstraint.builder()
                .version("^2.0.0")
                .build();
        final PluginVersionConstraint pluginVersionConstraint = PluginVersionConstraint.builder()
                .pluginId("unique-id")
                .version("^1.0.0")
                .build();
        final ImmutableSet<Constraint> requiredConstraints = ImmutableSet.of(graylogVersionConstraint, pluginVersionConstraint);
        assertThat(constraintChecker.checkConstraints(requiredConstraints).stream().allMatch(c -> c.fulfilled())).isTrue();
    }

    @Test
    public void checkConstraintsFails() {
        final TestPluginMetaData pluginMetaData = new TestPluginMetaData();
        final PluginVersionConstraintChecker constraintChecker = new PluginVersionConstraintChecker(Collections.singleton(pluginMetaData));

        final GraylogVersionConstraint graylogVersionConstraint = GraylogVersionConstraint.builder()
                .version("^2.0.0")
                .build();
        final PluginVersionConstraint pluginVersionConstraint = PluginVersionConstraint.builder()
                .pluginId("unique-id")
                .version("^2.0.0")
                .build();
        final ImmutableSet<Constraint> requiredConstraints = ImmutableSet.of(graylogVersionConstraint, pluginVersionConstraint);
        assertThat(constraintChecker.checkConstraints(requiredConstraints).stream().allMatch(c -> !c.fulfilled())).isTrue();
    }

    private static final class TestPluginMetaData implements PluginMetaData {
        @Override
        public String getUniqueId() {
            return "unique-id";
        }

        @Override
        public String getName() {
            return "name";
        }

        @Override
        public String getAuthor() {
            return "author";
        }

        @Override
        public URI getURL() {
            return URI.create("https://www.graylog.org/");
        }

        @Override
        public Version getVersion() {
            return Version.from(1, 2, 3);
        }

        @Override
        public String getDescription() {
            return "description";
        }

        @Override
        public Version getRequiredVersion() {
            return Version.from(1, 0, 0);
        }

        @Override
        public Set<ServerStatus.Capability> getRequiredCapabilities() {
            return Collections.singleton(ServerStatus.Capability.PRIMARY);
        }
    }
}
