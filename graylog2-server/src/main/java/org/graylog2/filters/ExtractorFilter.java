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
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.inputs.Extractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ExtractorFilter implements MessageFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractorFilter.class);

    private static final String NAME = "Extractor";

    private Cache<String, List<Extractor>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build();

    private final InputService inputService;

    @Inject
    public ExtractorFilter(InputService inputService) {
        this.inputService = inputService;
    }

    @Override
    public boolean filter(Message msg) {
        if (msg.getSourceInput() == null) {
            return false;
        }

        for (Extractor extractor : loadExtractors(msg.getSourceInput().getId())) {
            try {
                extractor.runExtractor(msg);
            } catch (Exception e) {
                extractor.incrementExceptions();
                LOG.error("Could not apply extractor.", e);
                continue;
            }
        }

        return false;
    }

    private List<Extractor> loadExtractors(final String inputId) {
        try {
            return cache.get(inputId, new Callable<List<Extractor>>() {
                @Override
                public List<Extractor> call() throws Exception {
                    LOG.debug("Re-loading extractors for input <{}> into cache.", inputId);

                    Input input = inputService.find(inputId);

                    List<Extractor> sorted = Lists.newArrayList(inputService.getExtractors(input));

                    Collections.sort(sorted, new Comparator<Extractor>() {
                        public int compare(Extractor e1, Extractor e2) {
                            return e1.getOrder().intValue() - e2.getOrder().intValue();
                        }
                    });

                    return sorted;
                }
            });
        } catch (ExecutionException e) {
            LOG.error("Could not load extractors into cache. Returning empty list.", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        // runs first of the built-in filters
        return 10;
    }

}
