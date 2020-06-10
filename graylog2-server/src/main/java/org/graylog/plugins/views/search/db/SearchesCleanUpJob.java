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
package org.graylog.plugins.views.search.db;

import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchesCleanUpJob extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(SearchesCleanUpJob.class);

    private final ViewService viewService;
    private final SearchDbService searchDbService;
    private final Instant mustNotBeOlderThan;

    @Inject
    public SearchesCleanUpJob(ViewService viewService,
                              SearchDbService searchDbService,
                              @Named("views_maximum_search_age") Duration maximumSearchAge) {
        this.viewService = viewService;
        this.searchDbService = searchDbService;
        this.mustNotBeOlderThan = Instant.now().minus(maximumSearchAge);
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
    public boolean parentOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 3600;
    }

    @Override
    public int getPeriodSeconds() {
        return Duration.standardHours(8).toStandardSeconds().getSeconds();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        final Set<String> requiredIds = viewService.streamAll().map(ViewDTO::searchId).collect(Collectors.toSet());
        searchDbService.streamAll()
                .filter(search -> search.createdAt().isBefore(mustNotBeOlderThan) && !requiredIds.contains(search.id()))
                .forEach(search -> searchDbService.delete(search.id()));
    }
}
