/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.filters;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import org.graylog2.filters.blacklist.FilterDescription;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RulesEngine;
import org.graylog2.plugin.filters.MessageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class RulesFilter implements MessageFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RulesFilter.class);

    private final FilterService filterService;
    private final RulesEngine.RulesSession privateSession;
    private final Cache<String, Set<FilterDescription>> cache;
    private Set<FilterDescription> currentFilterSet;

    @Inject
    public RulesFilter(RulesEngine rulesEngine, final FilterService filterService) {
        this.filterService = filterService;

        currentFilterSet = Sets.newHashSet();
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();
        privateSession = rulesEngine.createPrivateSession();
    }

    @Override
    public boolean filter(Message msg) {
        final Set<FilterDescription> filters;
        try {
            filters = cache.get("filters", new Callable<Set<FilterDescription>>() {
                @Override
                public Set<FilterDescription> call() throws Exception {
                    // TODO this should be improved by computing the difference between the filter sets
                    // most of the time nothing changes at all.
                    final Set<FilterDescription> newFilters = filterService.loadAll();
                    final Sets.SetView<FilterDescription> difference =
                            Sets.symmetricDifference(currentFilterSet, newFilters);
                    if (difference.isEmpty()) {
                        // there wasn't any change, simply return the current filter set
                        LOG.debug("Filter sets are identical, not updating rules engine.");
                        return currentFilterSet;
                    }

                    // something changed, we simply update everything, not trying to do the minimal changes yet
                    // should this become too expensive we need to do something smarter
                    LOG.debug("Updating rules engine, filter sets differ: {}", difference);
                    // retract all current filter facts
                    for (FilterDescription filterDescription : currentFilterSet) {
                        privateSession.deleteFact(filterDescription);
                    }
                    // state all new filter facts
                    for (FilterDescription filterDescription : newFilters) {
                        privateSession.insertFact(filterDescription);
                    }
                    currentFilterSet.clear();
                    // remember currently stated facts
                    currentFilterSet.addAll(newFilters);
                    return currentFilterSet;
                }
            });
        } catch (ExecutionException ignored) {
            return false;
        }

        // Always run the rules engine to make sure rules from the external rules file will be run.
        privateSession.evaluate(msg, true);

        // false if not explicitly set to true in the rules.
        return msg.getFilterOut();
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
