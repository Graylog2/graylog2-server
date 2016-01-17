package org.graylog.plugins.messageprocessor.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.Rule;
import org.graylog.plugins.messageprocessor.ast.statements.Statement;
import org.graylog.plugins.messageprocessor.db.RuleSourceService;
import org.graylog.plugins.messageprocessor.events.RulesChangedEvent;
import org.graylog.plugins.messageprocessor.parser.ParseException;
import org.graylog.plugins.messageprocessor.parser.RuleParser;
import org.graylog.plugins.messageprocessor.rest.RuleSource;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.cache.CacheLoader.asyncReloading;

public class NaiveRuleProcessor implements MessageProcessor {
    private static final Logger log = LoggerFactory.getLogger(NaiveRuleProcessor.class);

    private final Journal journal;
    private final Meter filteredOutMessages;
    private final LoadingCache<String, Rule> ruleCache;

    @Inject
    public NaiveRuleProcessor(RuleSourceService ruleSourceService,
                              RuleParser ruleParser,
                              Journal journal,
                              MetricRegistry metricRegistry,
                              @Named("daemonScheduler") ScheduledExecutorService scheduledExecutorService,
                              @ClusterEventBus EventBus clusterBus) {
        this.journal = journal;
        this.filteredOutMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "filteredOutMessages"));
        clusterBus.register(this);
        ruleCache = CacheBuilder.newBuilder()
                .build(asyncReloading(new RuleLoader(ruleSourceService, ruleParser), scheduledExecutorService));
        // prime the cache with all presently stored rules
        try {
            ruleCache.getAll(Collections.emptyList());
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
        private final RuleParser ruleParser;

        public RuleLoader(RuleSourceService ruleSourceService, RuleParser ruleParser) {
            this.ruleSourceService = ruleSourceService;
            this.ruleParser = ruleParser;
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
                    all.put(ruleSource.id(), ruleParser.parseRule(ruleSource.source()));
                } catch (ParseException e) {
                    log.error("Unable to parse rule: " + e.getMessage());
                }
            }
            return all;
        }

        @Override
        public Rule load(@Nullable String ruleId) throws Exception {
            final RuleSource ruleSource = ruleSourceService.load(ruleId);
            try {
                return ruleParser.parseRule(ruleSource.source());
            } catch (ParseException e) {
                log.error("Unable to parse rule: " + e.getMessage());
                throw e;
            }
        }
    }
}
