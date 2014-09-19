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
package org.graylog2.bundles;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.dashboards.DashboardRegistry;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.Indexer;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BundleExporter {


    private static final Logger LOG = LoggerFactory.getLogger(BundleExporter.class);

    private final InputService inputService;
    private final InputRegistry inputRegistry;
    private final ExtractorFactory extractorFactory;
    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final OutputService outputService;
    private final DashboardService dashboardService;
    private final DashboardRegistry dashboardRegistry;
    private final ServerStatus serverStatus;
    private final MetricRegistry metricRegistry;
    private final Indexer indexer;

    private final Map<String, MessageInput> createdInputs = new HashMap<>();
    private final Map<String, org.graylog2.plugin.streams.Output> createdOutputs = new HashMap<>();
    private final Map<String, org.graylog2.plugin.streams.Stream> createdStreams = new HashMap<>();
    private final Map<String, org.graylog2.dashboards.Dashboard> createdDashboards = new HashMap<>();
    private final Map<String, org.graylog2.plugin.streams.Output> outputsByReferenceId = new HashMap<>();
    private final Map<String, org.graylog2.plugin.streams.Stream> streamsByReferenceId = new HashMap<>();

    @Inject
    public BundleExporter(final InputService inputService,
                          final InputRegistry inputRegistry,
                          final ExtractorFactory extractorFactory,
                          final StreamService streamService,
                          final StreamRuleService streamRuleService,
                          final OutputService outputService,
                          final DashboardService dashboardService,
                          final DashboardRegistry dashboardRegistry,
                          final ServerStatus serverStatus,
                          final MetricRegistry metricRegistry,
                          final Indexer indexer) {
        this.inputService = inputService;
        this.inputRegistry = inputRegistry;
        this.extractorFactory = extractorFactory;
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.outputService = outputService;
        this.dashboardService = dashboardService;
        this.dashboardRegistry = dashboardRegistry;
        this.serverStatus = serverStatus;
        this.metricRegistry = metricRegistry;
        this.indexer = indexer;
    }

    public ConfigurationBundle export(final ExportBundle exportBundle) {
        final ConfigurationBundle configurationBundle = new ConfigurationBundle();

        List<Input> inputs = exportInputs(exportBundle.getInputs());
        List<Stream> streams = exportStreams(exportBundle.getStreams());
        List<Output> outputs = exportOutputs(exportBundle.getOutputs());
        List<Dashboard> dashboards = exportDashboards(exportBundle.getDashboards());

        configurationBundle.setName(exportBundle.getName());
        configurationBundle.setCategory(exportBundle.getCategory());
        configurationBundle.setDescription(exportBundle.getDescription());
        configurationBundle.setInputs(inputs);
        configurationBundle.setStreams(streams);
        configurationBundle.setOutputs(outputs);
        configurationBundle.setDashboards(dashboards);

        return configurationBundle;
    }

    private List<Input> exportInputs(final List<String> inputs) {
        final ImmutableList.Builder<Input> inputBuilder = ImmutableList.builder();

        for (String inputId : inputs) {
            final Input input = exportInput(inputId);
            if (input != null) {
                inputBuilder.add(exportInput(inputId));
            }
        }

        return inputBuilder.build();
    }

    private Input exportInput(final String inputId) {
        final org.graylog2.inputs.Input input;
        try {
            input = inputService.find(inputId);
        } catch (NotFoundException e) {
            LOG.debug("Requested input {} not found.", inputId);
            return null;
        }

        final Input inputDescription = new Input();
        inputDescription.setTitle(input.getTitle());
        inputDescription.setType(input.getType());
        inputDescription.setGlobal(input.isGlobal());
        inputDescription.setConfiguration(input.getConfiguration());
        inputDescription.setStaticFields(input.getStaticFields());
        inputDescription.setExtractors(exportExtractors(input));

        return inputDescription;
    }

    private List<Extractor> exportExtractors(org.graylog2.inputs.Input input) {
        final ImmutableList.Builder<Extractor> extractorBuilder = ImmutableList.builder();
        final List<org.graylog2.plugin.inputs.Extractor> extractors = inputService.getExtractors(input);

        for (org.graylog2.plugin.inputs.Extractor extractor : extractors) {
            extractorBuilder.add(exportExtractor(extractor));
        }

        return extractorBuilder.build();
    }

    private Extractor exportExtractor(final org.graylog2.plugin.inputs.Extractor extractor) {
        final Extractor extractorDescription = new Extractor();

        extractorDescription.setTitle(extractor.getTitle());
        extractorDescription.setType(extractor.getType());
        extractorDescription.setConfiguration(extractor.getExtractorConfig());
        extractorDescription.setConditionType(extractor.getConditionType());
        extractorDescription.setConditionValue(extractor.getConditionValue());
        extractorDescription.setCursorStrategy(extractor.getCursorStrategy());
        extractorDescription.setOrder(extractor.getOrder().intValue());
        extractorDescription.setSourceField(extractor.getSourceField());
        extractorDescription.setTargetField(extractor.getTargetField());

        final List<org.graylog2.plugin.inputs.Converter> converters = extractor.getConverters();
        extractorDescription.setConverters(exportConverters(converters));

        return extractorDescription;
    }

    private List<Converter> exportConverters(final List<org.graylog2.plugin.inputs.Converter> converters) {
        final ImmutableList.Builder<Converter> converterBuilder = ImmutableList.builder();
        for (org.graylog2.plugin.inputs.Converter converter : converters) {
            final Converter converterDescription = new Converter();
            final org.graylog2.plugin.inputs.Converter.Type type =
                    org.graylog2.plugin.inputs.Converter.Type.valueOf(converter.getType().toUpperCase());

            converterDescription.setType(type);
            converterDescription.setConfiguration(converter.getConfig());

            converterBuilder.add(converterDescription);
        }

        return converterBuilder.build();
    }

    private List<Stream> exportStreams(final List<String> streams) {
        final ImmutableList.Builder<Stream> streamBuilder = ImmutableList.builder();

        for (String streamId : streams) {
            final Stream stream = exportStream(streamId);

            if (stream != null) {
                streamBuilder.add(stream);
            }
        }

        return streamBuilder.build();
    }

    private Stream exportStream(final String streamId) {
        final org.graylog2.plugin.streams.Stream stream;
        try {
            stream = streamService.load(streamId);
        } catch (NotFoundException e) {
            LOG.debug("Requested stream {} not found.", streamId);
            return null;
        }

        final Stream streamDescription = new Stream();
        streamDescription.setTitle(stream.getTitle());
        streamDescription.setDescription(stream.getDescription());
        streamDescription.setDisabled(stream.getDisabled());
        streamDescription.setOutputs(exportOutputReferences(stream.getOutputs()));
        streamDescription.setStreamRules(exportStreamRules(stream.getStreamRules()));

        return streamDescription;
    }

    private List<StreamRule> exportStreamRules(List<org.graylog2.plugin.streams.StreamRule> streamRules) {
        final ImmutableList.Builder<StreamRule> streamRuleBuilder = ImmutableList.builder();

        for (org.graylog2.plugin.streams.StreamRule streamRule : streamRules) {
            final StreamRule streamRuleDescription = new StreamRule();
            streamRuleDescription.setType(streamRule.getType());
            streamRuleDescription.setField(streamRule.getField());
            streamRuleDescription.setValue(streamRule.getValue());
            streamRuleDescription.setInverted(streamRule.getInverted());

            streamRuleBuilder.add(streamRuleDescription);
        }

        return streamRuleBuilder.build();
    }

    private Set<String> exportOutputReferences(Set<org.graylog2.plugin.streams.Output> outputs) {
        final ImmutableSet.Builder<String> outputBuilder = ImmutableSet.builder();

        for (org.graylog2.plugin.streams.Output output : outputs) {
            outputBuilder.add(output.getId());
        }

        return outputBuilder.build();
    }

    private List<Output> exportOutputs(final List<String> outputs) {
        final ImmutableList.Builder<Output> outputBuilder = ImmutableList.builder();

        for (String outputId : outputs) {
            final Output output = exportOutput(outputId);
            if (output != null) {
                outputBuilder.add(output);
            }
        }

        return outputBuilder.build();
    }

    private Output exportOutput(final String outputId) {
        final org.graylog2.plugin.streams.Output output;

        try {
            output = outputService.load(outputId);
        } catch (NotFoundException e) {
            LOG.debug("Requested output {} not found.", outputId);
            return null;
        }

        final Output outputDescription = new Output();

        outputDescription.setId(output.getId());
        outputDescription.setTitle(output.getTitle());
        outputDescription.setType(output.getType());
        outputDescription.setConfiguration(output.getConfiguration());

        return outputDescription;
    }

    private List<Dashboard> exportDashboards(final List<String> dashboards) {
        final ImmutableList.Builder<Dashboard> dashboardBuilder = ImmutableList.builder();

        for (String dashboardId : dashboards) {
            final Dashboard dashboard = exportDashboard(dashboardId);

            if (dashboard != null) {
                dashboardBuilder.add(dashboard);
            }
        }

        return dashboardBuilder.build();
    }

    private Dashboard exportDashboard(String dashboardId) {
        final org.graylog2.dashboards.Dashboard dashboard;
        try {
            dashboard = dashboardService.load(dashboardId);
        } catch (NotFoundException e) {
            LOG.debug("Requested dashboard {} not found.", dashboardId);
            return null;
        }

        final Dashboard dashboardDescription = new Dashboard();
        dashboardDescription.setTitle(dashboard.getTitle());
        dashboardDescription.setDescription(dashboard.getDescription());
        dashboardDescription.setDashboardWidgets(exportDashboardWidgets(dashboard));

        return dashboardDescription;
    }

    private List<DashboardWidget> exportDashboardWidgets(final org.graylog2.dashboards.Dashboard dashboard) {
        final ImmutableList.Builder<DashboardWidget> dashboardWidgetBuilder = ImmutableList.builder();

        // Add all widgets of this dashboard.
        final Map<String, Object> fields = dashboard.getFields();
        @SuppressWarnings("unchecked")
        final Map<String, Object> positions = (Map<String, Object>) dashboard.asMap().get("positions");
        if (fields.containsKey(DashboardImpl.EMBEDDED_WIDGETS)) {
            for (BasicDBObject widgetFields : (List<BasicDBObject>) fields.get(DashboardImpl.EMBEDDED_WIDGETS)) {
                org.graylog2.dashboards.widgets.DashboardWidget widget = null;
                try {
                    widget = org.graylog2.dashboards.widgets.DashboardWidget.fromPersisted(null, null, widgetFields);
                } catch (Exception e) {
                    LOG.warn("Error while exporting widgets of dashboard " + dashboard.getId(), e);
                    continue;
                }

                final DashboardWidget dashboardWidgetDescription = new DashboardWidget();
                final Map<String, Object> widgetConfig = widget.getConfig();

                dashboardWidgetDescription.setDescription(widget.getDescription());
                dashboardWidgetDescription.setType(widget.getType());
                dashboardWidgetDescription.setConfiguration(widgetConfig);
                dashboardWidgetDescription.setCacheTime(widget.getCacheTime());
                dashboardWidgetDescription.setStream((String) widgetConfig.get("stream_id"));

                @SuppressWarnings("unchecked")
                final Map<String, Integer> widgetPosition = (Map<String, Integer>) positions.get(widget.getId());
                final Integer row = widgetPosition.get("row");
                final Integer col = widgetPosition.get("col");
                dashboardWidgetDescription.setRow(row == null ? 0 : row);
                dashboardWidgetDescription.setCol(col == null ? 0 : col);

                dashboardWidgetBuilder.add(dashboardWidgetDescription);
            }
        }

        return dashboardWidgetBuilder.build();
    }
}