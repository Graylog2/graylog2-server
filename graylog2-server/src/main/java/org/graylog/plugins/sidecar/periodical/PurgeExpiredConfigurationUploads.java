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
package org.graylog.plugins.sidecar.periodical;

import org.graylog.plugins.sidecar.services.ImportService;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class PurgeExpiredConfigurationUploads extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(PurgeExpiredConfigurationUploads.class);

    private final ImportService importService;

    @Inject
    public PurgeExpiredConfigurationUploads(ImportService importService) {
        this.importService = importService;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
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
        return 60 * 10;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        final Period outOfUseThreshold = new Period().withDays(30);
        final int purgedUploads = importService.destroyExpired(outOfUseThreshold);
        LOG.debug("Purged {} outdated configuration uploads.", purgedUploads);
    }
}
