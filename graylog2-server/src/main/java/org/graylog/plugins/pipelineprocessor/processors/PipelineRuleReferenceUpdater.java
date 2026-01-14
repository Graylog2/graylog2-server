/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.processors;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineSource;
import org.graylog.plugins.pipelineprocessor.rest.PipelineUtils;
import org.graylog.plugins.pipelineprocessor.rest.StageSource;
import org.graylog2.database.NotFoundException;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory.createDefaultRateLimitedLog;

/**
 * Handles automatic updates of pipeline rule references when rules are renamed or deleted.
 * This ensures pipelines don't contain invalid rule references, addressing issue #21162.
 */
@Singleton
public class PipelineRuleReferenceUpdater {
    private static final RateLimitedLog log = createDefaultRateLimitedLog(PipelineRuleReferenceUpdater.class);

    private final PipelineService pipelineService;
    private final RuleService ruleService;
    private final PipelineRuleParser pipelineRuleParser;

    @Inject
    public PipelineRuleReferenceUpdater(
            PipelineService pipelineService,
            RuleService ruleService,
            PipelineRuleParser pipelineRuleParser,
            EventBus serverEventBus) {
        this.pipelineService = pipelineService;
        this.ruleService = ruleService;
        this.pipelineRuleParser = pipelineRuleParser;

        // Register to listen for rule changes
        serverEventBus.register(this);

        log.info("PipelineRuleReferenceUpdater initialized");
    }

    /**
     * Handles rule changes (updates and deletions).
     * When a rule is renamed, updates all pipeline references from old name to new name.
     * When a rule is deleted, removes all references from pipelines.
     */
    @Subscribe
    public void handleRuleChanges(RulesChangedEvent event) {
        // Handle rule renames (updates with oldTitle)
        event.updatedRules().stream()
                .filter(ref -> ref.oldTitle() != null && !ref.oldTitle().equals(ref.title()))
                .forEach(ref -> {
                    log.info("Rule renamed from '{}' to '{}', updating pipeline references", ref.oldTitle(), ref.title());
                    updateRuleReferencesInAllPipelines(ref.oldTitle(), ref.title());
                });

        // Handle rule deletions
        event.deletedRules().forEach(ref -> {
            log.info("Rule '{}' deleted, removing references from pipelines", ref.title());
            removeRuleReferencesFromAllPipelines(ref.title());
        });
    }

    /**
     * Updates all occurrences of a rule reference in all pipelines.
     *
     * @param oldRuleName the old rule name to find
     * @param newRuleName the new rule name to replace with
     */
    private void updateRuleReferencesInAllPipelines(String oldRuleName, String newRuleName) {
        Collection<PipelineDao> pipelines = pipelineService.loadAll();
        int updatedCount = 0;

        for (PipelineDao pipeline : pipelines) {
            try {
                // Check if this pipeline references the old rule name
                if (!pipelineReferencesRule(pipeline, oldRuleName)) {
                    continue;
                }

                PipelineSource pipelineSource = PipelineSource.fromDao(pipelineRuleParser, pipeline);

                // Check if any stage contains the old rule name
                boolean modified = false;
                for (StageSource stage : pipelineSource.stages()) {
                    if (stage.rules().contains(oldRuleName)) {
                        modified = true;
                        break;
                    }
                }

                if (modified) {
                    // Replace old rule name with new rule name in the source string
                    String updatedSource = replaceRuleNameInSource(pipeline.source(), oldRuleName, newRuleName);

                    // Save the updated pipeline
                    PipelineDao updatedPipeline = pipeline.toBuilder()
                            .source(updatedSource)
                            .build();
                    pipelineService.save(updatedPipeline, false);
                    updatedCount++;

                    log.info("Updated pipeline '{}' ({}): renamed rule reference from '{}' to '{}'",
                            pipeline.title(), pipeline.id(), oldRuleName, newRuleName);
                }
            } catch (Exception e) {
                log.error("Failed to update rule reference in pipeline '{}' ({}): {}",
                        pipeline.title(), pipeline.id(), e.getMessage(), e);
            }
        }

        if (updatedCount > 0) {
            log.info("Successfully updated rule references in {} pipeline(s)", updatedCount);
        }
    }

    /**
     * Removes all occurrences of a rule reference from all pipelines.
     *
     * @param ruleName the rule name to remove
     */
    private void removeRuleReferencesFromAllPipelines(String ruleName) {
        Collection<PipelineDao> pipelines = pipelineService.loadAll();
        int updatedCount = 0;

        for (PipelineDao pipeline : pipelines) {
            try {
                // Check if this pipeline references the rule
                if (!pipelineReferencesRule(pipeline, ruleName)) {
                    continue;
                }

                PipelineSource pipelineSource = PipelineSource.fromDao(pipelineRuleParser, pipeline);

                // Check if any stage contains the rule
                boolean modified = false;
                for (StageSource stage : pipelineSource.stages()) {
                    if (stage.rules().contains(ruleName)) {
                        // Remove the rule from the stage
                        List<String> updatedRules = stage.rules().stream()
                                .filter(rule -> !rule.equals(ruleName))
                                .toList();

                        if (updatedRules.size() != stage.rules().size()) {
                            modified = true;
                        }
                    }
                }

                if (modified) {
                    // Remove the rule reference from the source string
                    String updatedSource = removeRuleNameFromSource(pipeline.source(), ruleName);

                    // Save the updated pipeline
                    PipelineDao updatedPipeline = pipeline.toBuilder()
                            .source(updatedSource)
                            .build();
                    pipelineService.save(updatedPipeline, false);
                    updatedCount++;

                    log.info("Updated pipeline '{}' ({}): removed rule reference '{}'",
                            pipeline.title(), pipeline.id(), ruleName);
                }
            } catch (Exception e) {
                log.error("Failed to remove rule reference from pipeline '{}' ({}): {}",
                        pipeline.title(), pipeline.id(), e.getMessage(), e);
            }
        }

        if (updatedCount > 0) {
            log.info("Successfully removed rule references from {} pipeline(s)", updatedCount);
        }
    }

    /**
     * Checks if a pipeline references a specific rule by name.
     */
    private boolean pipelineReferencesRule(PipelineDao pipeline, String ruleName) {
        // Simple string contains check - more efficient than parsing
        return pipeline.source().contains(ruleName);
    }

    /**
     * Replaces a rule name in the pipeline source string.
     * Uses regex to ensure we only replace exact rule name matches, not partial matches.
     */
    private String replaceRuleNameInSource(String source, String oldRuleName, String newRuleName) {
        // Escape special regex characters in rule names
        String escapedOldName = Pattern.quote(oldRuleName);

        // Match the rule name as a whole word (surrounded by quotes or whitespace)
        // This ensures we don't replace partial matches within other rule names
        String pattern = "(?<=[\"\\s])(" + escapedOldName + ")(?=[\"\\s,;])";

        return source.replaceAll(pattern, newRuleName);
    }

    /**
     * Removes a rule name from the pipeline source string.
     * Handles comma-separated rule lists properly.
     */
    private String removeRuleNameFromSource(String source, String ruleName) {
        String escapedRuleName = Pattern.quote(ruleName);

        // Remove rule with optional trailing comma and spaces
        // Pattern matches: "ruleName", or "ruleName" at end of list
        String pattern1 = "\"" + escapedRuleName + "\"\\s*,\\s*";
        String pattern2 = ",\\s*\"" + escapedRuleName + "\"";
        String pattern3 = "\"" + escapedRuleName + "\"";

        // Try removing with trailing comma first
        String result = source.replaceAll(pattern1, "");
        // Then try removing with leading comma
        result = result.replaceAll(pattern2, "");
        // Finally try removing standalone (single rule in stage)
        result = result.replaceAll(pattern3, "");

        return result;
    }
}
