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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GraylogVersionConstraintCheckerTest {
    @Test
    public void checkConstraints() {
        final GraylogVersionConstraintChecker constraintChecker = new GraylogVersionConstraintChecker("1.0.0");

        final GraylogVersionConstraint graylogVersionConstraint = GraylogVersionConstraint.builder()
                .version("^1.0.0")
                .build();
        final PluginVersionConstraint pluginVersionConstraint = PluginVersionConstraint.builder()
                .pluginId("unique-id")
                .version("^1.0.0")
                .build();
        final ImmutableSet<Constraint> requiredConstraints = ImmutableSet.of(graylogVersionConstraint, pluginVersionConstraint);
        assertThat(constraintChecker.checkConstraints(requiredConstraints)).containsOnly(graylogVersionConstraint);
    }

    @Test
    public void checkConstraintsFails() {
        final GraylogVersionConstraintChecker constraintChecker = new GraylogVersionConstraintChecker("1.0.0");

        final GraylogVersionConstraint graylogVersionConstraint = GraylogVersionConstraint.builder()
                .version("^2.0.0")
                .build();
        final PluginVersionConstraint pluginVersionConstraint = PluginVersionConstraint.builder()
                .pluginId("unique-id")
                .version("^1.0.0")
                .build();
        final ImmutableSet<Constraint> requiredConstraints = ImmutableSet.of(graylogVersionConstraint, pluginVersionConstraint);
        assertThat(constraintChecker.checkConstraints(requiredConstraints)).isEmpty();
    }
}