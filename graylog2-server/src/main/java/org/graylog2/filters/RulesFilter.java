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
package org.graylog2.filters;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.database.NotFoundException;
import org.graylog2.filters.events.FilterDescriptionUpdateEvent;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RulesEngine;
import org.graylog2.plugin.filters.MessageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class RulesFilter implements MessageFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RulesFilter.class);

    private final RulesEngine rulesEngine;
    private final FilterService filterService;
    private final ScheduledExecutorService scheduler;
    private final AtomicReference<RulesEngine.RulesSession> privateSession = new AtomicReference<>(null);

    @Inject
    public RulesFilter(final RulesEngine rulesEngine,
                       final FilterService filterService,
                       final EventBus serverEventBus,
                       @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        this.rulesEngine = rulesEngine;
        this.filterService = filterService;
        this.scheduler = scheduler;

        loadRules();

        // TODO: This class needs lifecycle management to avoid leaking objects in the EventBus
        serverEventBus.register(this);
    }

    @Override
    public boolean filter(Message msg) {
        // Always run the rules engine to make sure rules from the external rules file will be run.
        privateSession.get().evaluate(msg, true);

        // false if not explicitly set to true in the rules.
        return msg.getFilterOut();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleRulesUpdate(FilterDescriptionUpdateEvent ignored) {
        LOG.debug("Updating filter descriptions: {}", ignored);
        scheduler.submit(this::loadRules);
    }

    private void loadRules() {
        LOG.debug("Loading rule filters");
        try {
            final RulesEngine.RulesSession newSession = rulesEngine.createPrivateSession();

            filterService.loadAll().forEach(filterDescription -> {
                LOG.debug("Insert filter description: {}", filterDescription);
                newSession.insertFact(filterDescription);
            });

            privateSession.set(newSession);
        } catch (NotFoundException e) {
            LOG.error("No filters found", e);
        }
    }

    @Override
    public String getName() {
        return "Rulesfilter";
    }

    @Override
    public int getPriority() {
        // runs third of the built-in filters
        return 30;
    }

}
