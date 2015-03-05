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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StaticFieldFilter implements MessageFilter {

    private static final Logger LOG = LoggerFactory.getLogger(StaticFieldFilter.class);

    private static final String NAME = "Static field appender";

    private final Cache<String, List<Map.Entry<String, String>>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build();

    private final InputService inputService;

    @Inject
    public StaticFieldFilter(InputService inputService) {
        this.inputService = inputService;
    }

    @Override
    public boolean filter(Message msg) {
        if (msg.getSourceInputId() == null)
            return false;

        for(final Map.Entry<String, String> field : loadStaticFields(msg.getSourceInputId())) {
            if(!msg.hasField(field.getKey())) {
                msg.addField(field.getKey(), field.getValue());
            } else {
                LOG.debug("Message already contains field [{}]. Not overwriting.", field.getKey());
            }
        }

        return false;
    }

    private List<Map.Entry<String, String>> loadStaticFields(final String inputId) {
        try {
            return cache.get(inputId, new Callable<List<Map.Entry<String, String>>>() {
                @Override
                public List<Map.Entry<String, String>> call() throws Exception {
                    LOG.debug("Re-loading static fields for input <{}> into cache.", inputId);
                    try {
                        final Input input = inputService.find(inputId);
                        return inputService.getStaticFields(input);
                    } catch (NotFoundException e) {
                        LOG.warn("Unable to load input: {}", e.getMessage());
                        return Collections.emptyList();
                    }
                }
            });
        } catch (ExecutionException e) {
            LOG.error("Could not load static fields into cache. Returning empty list.", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        // runs second of the built-in filters
        return 20;
    }

}
