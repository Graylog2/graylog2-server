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
package org.graylog2.bindings.providers;

import org.graylog2.bundles.BundleImporter;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.timeranges.TimeRangeFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class BundleImporterProvider implements Provider<BundleImporter> {

    private final InputService inputService;
    private final InputRegistry inputRegistry;
    private final ExtractorFactory extractorFactory;
    private final ConverterFactory converterFactory;
    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final IndexSetRegistry indexSetRegistry;
    private final OutputService outputService;
    private final DashboardService dashboardService;
    private final DashboardWidgetCreator dashboardWidgetCreator;
    private final ServerStatus serverStatus;
    private final MessageInputFactory messageInputFactory;
    private final InputLauncher inputLauncher;
    private final GrokPatternService grokPatternService;
    private final TimeRangeFactory timeRangeFactory;
    private final ClusterEventBus clusterBus;

    @Inject
    public BundleImporterProvider(final InputService inputService,
                                  final InputRegistry inputRegistry,
                                  final ExtractorFactory extractorFactory,
                                  final ConverterFactory converterFactory,
                                  final StreamService streamService,
                                  final StreamRuleService streamRuleService,
                                  final IndexSetRegistry indexSetRegistry,
                                  final OutputService outputService,
                                  final DashboardService dashboardService,
                                  final DashboardWidgetCreator dashboardWidgetCreator,
                                  final ServerStatus serverStatus,
                                  final MessageInputFactory messageInputFactory,
                                  final InputLauncher inputLauncher,
                                  final GrokPatternService grokPatternService,
                                  final TimeRangeFactory timeRangeFactory,
                                  final ClusterEventBus clusterBus) {
        this.inputService = inputService;
        this.inputRegistry = inputRegistry;
        this.extractorFactory = extractorFactory;
        this.converterFactory = converterFactory;
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.indexSetRegistry = indexSetRegistry;
        this.outputService = outputService;
        this.dashboardService = dashboardService;
        this.dashboardWidgetCreator = dashboardWidgetCreator;
        this.serverStatus = serverStatus;
        this.messageInputFactory = messageInputFactory;
        this.inputLauncher = inputLauncher;
        this.grokPatternService = grokPatternService;
        this.timeRangeFactory = timeRangeFactory;
        this.clusterBus = clusterBus;
    }

    @Override
    public BundleImporter get() {
        return new BundleImporter(inputService, inputRegistry, extractorFactory, converterFactory,
                streamService, streamRuleService, indexSetRegistry, outputService, dashboardService,
                dashboardWidgetCreator, serverStatus, messageInputFactory,
                inputLauncher, grokPatternService, timeRangeFactory, clusterBus);
    }
}
