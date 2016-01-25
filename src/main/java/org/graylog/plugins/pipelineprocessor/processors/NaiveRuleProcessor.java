/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.statements.Statement;
import org.graylog.plugins.pipelineprocessor.db.RuleSourceService;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.RuleSource;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.journal.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.cache.CacheLoader.asyncReloading;

public class NaiveRuleProcessor implements MessageProcessor {
    private static final Logger log = LoggerFactory.getLogger(NaiveRuleProcessor.class);

    private final Journal journal;
    private final Meter filteredOutMessages;
    private final LoadingCache<String, Rule> ruleCache;

    @Inject
    public NaiveRuleProcessor(RuleSourceService ruleSourceService,
                              PipelineRuleParser pipelineRuleParser,
                              Journal journal,
                              MetricRegistry metricRegistry,
                              @Named("daemonScheduler") ScheduledExecutorService scheduledExecutorService,
                              @ClusterEventBus EventBus clusterBus) {
        this.journal = journal;
        this.filteredOutMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "filteredOutMessages"));
        clusterBus.register(this);
        ruleCache = CacheBuilder.newBuilder()
                .build(asyncReloading(new RuleLoader(ruleSourceService, pipelineRuleParser), scheduledExecutorService));
        // prime the cache with all presently stored rules
        try {
            final List<String> ruleIds = ruleSourceService.loadAll().stream().map(RuleSource::id).collect(Collectors.toList());
            log.info("Compiling {} processing rules", ruleIds.size());
            ruleCache.getAll(ruleIds);
        } catch (ExecutionException ignored) {}
    }

    @Override
    public Messages process(Messages messages) {
        for (Map.Entry<String, Rule> entry : ruleCache.asMap().entrySet()) {
            final Rule rule = entry.getValue();
            log.info("Evaluating rule {}", rule.name());

            for (Message message : messages) {
                try {
                    final EvaluationContext context = new EvaluationContext(message);
                    if (rule.when().evaluateBool(context)) {
                        log.info("[✓] Message {} matches condition", message.getId());

                        for (Statement statement : rule.then()) {
                            statement.evaluate(context);
                        }

                    } else {
                        log.info("[✕] Message {} does not match condition", message.getId());
                    }
                } catch (Exception e) {
                    log.error("Unable to process message", e);
                }
                if (message.getFilterOut()) {
                    log.info("[✝] Message {} was filtered out", message.getId());

                    filteredOutMessages.mark();
                    journal.markJournalOffsetCommitted(message.getJournalOffset());
                }
            }
        }
        return messages;
    }

    @Subscribe
    public void handleRuleChanges(RulesChangedEvent event) {
        event.deletedRuleIds().forEach(id -> {
            ruleCache.invalidate(id);
            log.info("Invalidated rule {}", id);
        });
        event.updatedRuleIds().forEach(id -> {
            ruleCache.refresh(id);
            log.info("Refreshing rule {}", id);
        });
    }

    private static class RuleLoader extends CacheLoader<String, Rule> {
        private final RuleSourceService ruleSourceService;
        private final PipelineRuleParser pipelineRuleParser;

        public RuleLoader(RuleSourceService ruleSourceService, PipelineRuleParser pipelineRuleParser) {
            this.ruleSourceService = ruleSourceService;
            this.pipelineRuleParser = pipelineRuleParser;
        }

        @Override
        public Map<String, Rule> loadAll(Iterable<? extends String> keys) throws Exception {
            final Map<String, Rule> all = Maps.newHashMap();
            final HashSet<String> keysToLoad = Sets.newHashSet(keys);
            for (RuleSource ruleSource : ruleSourceService.loadAll()) {
                if (!keysToLoad.isEmpty()) {
                    if (!keysToLoad.contains(ruleSource.id())) {
                        continue;
                    }
                }
                try {
                    all.put(ruleSource.id(), pipelineRuleParser.parseRule(ruleSource.source()));
                } catch (ParseException e) {
                    log.error("Unable to parse rule: " + e.getMessage());
                    all.put(ruleSource.id(), Rule.alwaysFalse("Failed to parse rule: " + ruleSource.id()));
                }
            }
            return all;
        }

        @Override
        public Rule load(@Nullable String ruleId) throws Exception {
            final RuleSource ruleSource = ruleSourceService.load(ruleId);
            try {
                return pipelineRuleParser.parseRule(ruleSource.source());
            } catch (ParseException e) {
                log.error("Unable to parse rule: " + e.getMessage());
                // return dummy rule
                return Rule.alwaysFalse("Failed to parse rule: " + ruleSource.id());
            }
        }
    }
}
