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

package org.graylog2.periodical;

import com.google.common.collect.ImmutableSortedSet;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.SortedSet;

public class ConfigurationManagementPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManagementPeriodical.class);
    private final SortedSet<Migration> migrations;

    @Inject
    public ConfigurationManagementPeriodical(Set<Migration> migrations) {
        this.migrations = ImmutableSortedSet.copyOf(migrations);
    }

    @Override
    public void doRun() {
        for(Migration migration : migrations) {
            try {
                migration.upgrade();
            } catch (Exception e) {
                LOG.error("Error while running migration <{}>", migration, e);
            }
        }
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean primaryOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
