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
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.dashboards.DashboardRegistry;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.KeywordRange;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.resources.dashboards.requests.WidgetPositionRequest;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

public class BundleImporter {
    private static final Logger LOG = LoggerFactory.getLogger(BundleImporter.class);

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
    private final Searches searches;

    private final Map<String, MessageInput> createdInputs = new HashMap<>();
    private final Map<String, org.graylog2.plugin.streams.Output> createdOutputs = new HashMap<>();
    private final Map<String, org.graylog2.plugin.streams.Stream> createdStreams = new HashMap<>();
    private final Map<String, org.graylog2.dashboards.Dashboard> createdDashboards = new HashMap<>();
    private final Map<String, org.graylog2.plugin.streams.Output> outputsByReferenceId = new HashMap<>();
    private final Map<String, org.graylog2.plugin.streams.Stream> streamsByReferenceId = new HashMap<>();

    @Inject
    public BundleImporter(final InputService inputService,
                          final InputRegistry inputRegistry,
                          final ExtractorFactory extractorFactory,
                          final StreamService streamService,
                          final StreamRuleService streamRuleService,
                          final OutputService outputService,
                          final DashboardService dashboardService,
                          final DashboardRegistry dashboardRegistry,
                          final ServerStatus serverStatus,
                          final MetricRegistry metricRegistry,
                          final Searches searches) {
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
        this.searches = searches;
    }

    public void runImport(final ConfigurationBundle bundle, final String userName) {
        final String bundleId = bundle.getId();

        try {
            createInputs(bundleId, bundle.getInputs(), userName);
            createOutputs(bundleId, bundle.getOutputs(), userName);
            createStreams(bundleId, bundle.getStreams(), userName);
            createDashboards(bundleId, bundle.getDashboards(), userName);
        } catch (Exception e) {
            LOG.error("Error while creating dashboards. Starting rollback.", e);
            if (!rollback()) {
                LOG.error("Rollback unsuccessful.");
            }
            Throwables.propagate(e);
        }
    }

    private boolean rollback() {
        boolean success = true;
        try {
            deleteCreatedDashboards();
        } catch (Exception e) {
            LOG.error("Error while removing dashboards during rollback.", e);
            success = false;
        }

        try {
            deleteCreatedStreams();
        } catch (Exception e) {
            LOG.error("Error while removing streams during rollback.", e);
            success = false;
        }

        try {
            deleteCreatedOutputs();
        } catch (Exception e) {
            LOG.error("Error while removing outputs during rollback.", e);
            success = false;
        }

        try {
            deleteCreatedInputs();
        } catch (Exception e) {
            LOG.error("Error while removing inputs during rollback.", e);
            success = false;
        }

        return success;
    }

    private void deleteCreatedInputs() throws NotFoundException {
        for (Map.Entry<String, MessageInput> entry : createdInputs.entrySet()) {
            final String inputId = entry.getKey();
            final MessageInput messageInput = entry.getValue();

            LOG.debug("Terminating message input {}", inputId);
            inputRegistry.terminate(messageInput);
            inputRegistry.cleanInput(messageInput);
        }
    }

    private void deleteCreatedOutputs() throws NotFoundException {
        for (Map.Entry<String, org.graylog2.plugin.streams.Output> entry : createdOutputs.entrySet()) {
            LOG.debug("Deleting output {} from database", entry.getKey());
            outputService.destroy(entry.getValue());
        }
    }

    private void deleteCreatedStreams() throws NotFoundException {
        for (Map.Entry<String, org.graylog2.plugin.streams.Stream> entry : createdStreams.entrySet()) {
            LOG.debug("Deleting stream {} from database", entry.getKey());
            streamService.destroy(entry.getValue());
        }
    }

    private void deleteCreatedDashboards() {
        for (Map.Entry<String, org.graylog2.dashboards.Dashboard> entry : createdDashboards.entrySet()) {
            final String dashboardId = entry.getKey();
            LOG.debug("Removing dashboard {} from registry", dashboardId);
            dashboardRegistry.remove(dashboardId);

            LOG.debug("Deleting dashboard {} from database", dashboardId);
            dashboardService.destroy(entry.getValue());
        }
    }

    private void createInputs(final String bundleId, final Set<Input> inputs, final String userName)
            throws org.graylog2.plugin.inputs.Extractor.ReservedFieldException, org.graylog2.ConfigurationException, NoSuchInputTypeException, ValidationException, ExtractorFactory.NoSuchExtractorException, NotFoundException, ConfigurationException {
        for (final Input input : inputs) {
            final MessageInput messageInput = createMessageInput(bundleId, input, userName);
            createdInputs.put(messageInput.getId(), messageInput);

            // Launch input. (this will run async and clean up itself in case of an error.)
            inputRegistry.launch(messageInput, messageInput.getId());
        }
    }

    private MessageInput createMessageInput(final String bundleId, final Input inputDescription, final String userName)
            throws NoSuchInputTypeException, ConfigurationException, ValidationException,
            NotFoundException, org.graylog2.ConfigurationException, ExtractorFactory.NoSuchExtractorException,
            org.graylog2.plugin.inputs.Extractor.ReservedFieldException {
        final Configuration inputConfig = new Configuration(inputDescription.getConfiguration());
        final DateTime createdAt = Tools.iso8601();

        final MessageInput messageInput = inputRegistry.create(inputDescription.getType(), inputConfig);
        messageInput.setTitle(inputDescription.getTitle());
        messageInput.setGlobal(inputDescription.isGlobal());
        messageInput.setCreatorUserId(userName);
        messageInput.setCreatedAt(createdAt);
        messageInput.setContentPack(bundleId);

        messageInput.setConfiguration(inputConfig);
        messageInput.checkConfiguration();

        // Don't run if exclusive and another instance is already running.
        if (messageInput.isExclusive() && inputRegistry.hasTypeRunning(messageInput.getClass())) {
            final String error = "Type is exclusive and already has input running.";
            LOG.error(error);
        }

        org.graylog2.inputs.Input mongoInput = inputService.create(
                buildMongoDbInput(UUID.randomUUID(), inputDescription, userName, createdAt, bundleId));

        // Persist input.
        final String persistId = inputService.save(mongoInput);
        messageInput.setPersistId(persistId);
        messageInput.initialize();

        addStaticFields(messageInput, inputDescription.getStaticFields());
        addExtractors(messageInput, inputDescription.getExtractors(), userName);

        return messageInput;
    }

    private void addExtractors(final MessageInput messageInput, final List<Extractor> extractors, final String userName)
            throws org.graylog2.plugin.inputs.Extractor.ReservedFieldException, org.graylog2.ConfigurationException,
            ExtractorFactory.NoSuchExtractorException, NotFoundException, ValidationException {

        for (Extractor extractor : extractors) {
            addExtractor(messageInput, extractor, userName);
        }
    }

    private void addExtractor(
            final MessageInput messageInput,
            final Extractor extractorDescription,
            final String userName)
            throws NotFoundException, ValidationException, org.graylog2.ConfigurationException,
            ExtractorFactory.NoSuchExtractorException, org.graylog2.plugin.inputs.Extractor.ReservedFieldException {
        if (extractorDescription.getSourceField().isEmpty() || extractorDescription.getTargetField().isEmpty()) {
            throw new ValidationException("Missing parameters source_field or target_field.");
        }

        final String extractorId = UUID.randomUUID().toString();
        final org.graylog2.plugin.inputs.Extractor extractor = extractorFactory.factory(
                extractorId,
                extractorDescription.getTitle(),
                extractorDescription.getOrder(),
                extractorDescription.getCursorStrategy(),
                extractorDescription.getType(),
                extractorDescription.getSourceField(),
                extractorDescription.getTargetField(),
                extractorDescription.getConfiguration(),
                userName,
                createConverters(extractorDescription.getConverters()),
                extractorDescription.getConditionType(),
                extractorDescription.getConditionValue()
        );

        org.graylog2.inputs.Input mongoInput = inputService.find(messageInput.getPersistId());
        inputService.addExtractor(mongoInput, extractor);
    }

    private List<org.graylog2.plugin.inputs.Converter> createConverters(final List<Converter> requestedConverters) {
        final ImmutableList.Builder<org.graylog2.plugin.inputs.Converter> converters = ImmutableList.builder();

        for (final Converter converter : requestedConverters) {
            try {
                converters.add(ConverterFactory.factory(converter.getType(), converter.getConfiguration()));
            } catch (ConverterFactory.NoSuchConverterException e) {
                LOG.warn("No such converter [" + converter.getType() + "]. Skipping.", e);
            } catch (org.graylog2.ConfigurationException e) {
                LOG.warn("Missing configuration for [" + converter.getType() + "]. Skipping.", e);
            }
        }

        return converters.build();
    }

    private void addStaticFields(final MessageInput messageInput, final Map<String, String> staticFields)
            throws NotFoundException, ValidationException {
        for (Map.Entry<String, String> staticField : staticFields.entrySet()) {
            addStaticField(messageInput, staticField.getKey(), staticField.getValue());
        }
    }

    private void addStaticField(final MessageInput messageInput, final String key, final String value)
            throws ValidationException, NotFoundException {
        // Check if key is a valid message key.
        if (!Message.validKey(key)) {
            final String errorMessage = "Invalid key: [" + key + "]";
            LOG.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (isNullOrEmpty(key) || isNullOrEmpty(value)) {
            final String errorMessage = "Missing attributes: key=[" + key + "], value=[" + value + "]";
            LOG.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        if (Message.RESERVED_FIELDS.contains(key) && !Message.RESERVED_SETTABLE_FIELDS.contains(key)) {
            final String errorMessage = "Cannot add static field. Field [" + key + "] is reserved.";
            LOG.error(errorMessage);
            throw new ValidationException(errorMessage);
        }

        messageInput.addStaticField(key, value);

        org.graylog2.inputs.Input mongoInput = inputService.find(messageInput.getPersistId());
        inputService.addStaticField(mongoInput, key, value);
    }

    private Map<String, Object> buildMongoDbInput(
            final UUID inputId,
            final Input input,
            final String userName,
            final DateTime createdAt,
            final String bundleId) {
        final ImmutableMap.Builder<String, Object> inputData = ImmutableMap.builder();
        inputData.put(MessageInput.FIELD_INPUT_ID, inputId.toString());
        inputData.put(MessageInput.FIELD_TITLE, input.getTitle());
        inputData.put(MessageInput.FIELD_TYPE, input.getType());
        inputData.put(MessageInput.FIELD_CREATOR_USER_ID, userName);
        inputData.put(MessageInput.FIELD_CONFIGURATION, input.getConfiguration());
        inputData.put(MessageInput.FIELD_CREATED_AT, createdAt);
        inputData.put(MessageInput.FIELD_CONTENT_PACK, bundleId);

        if (input.isGlobal()) {
            inputData.put(MessageInput.FIELD_GLOBAL, true);
        } else {
            inputData.put(MessageInput.FIELD_NODE_ID, serverStatus.getNodeId().toString());
        }

        return inputData.build();
    }

    private void createOutputs(final String bundleId, final Set<Output> outputs, final String userName)
            throws ValidationException {
        for (final Output outputDescription : outputs) {
            final OutputImpl output = createOutput(bundleId, outputDescription, userName);
            createdOutputs.put(output.getId(), output);
        }
    }

    private OutputImpl createOutput(final String bundleId, final Output outputDescription, final String userName)
            throws ValidationException {
        final String referenceId = outputDescription.getId();
        final OutputImpl output = (OutputImpl) outputService.create(new OutputImpl(
                outputDescription.getTitle(),
                outputDescription.getType(),
                outputDescription.getConfiguration(),
                Tools.iso8601().toDate(),
                userName,
                bundleId));

        if (!isNullOrEmpty(referenceId)) {
            outputsByReferenceId.put(referenceId, output);
        }

        return output;
    }

    private void createStreams(final String bundleId, final Set<Stream> streams, final String userName)
            throws ValidationException {
        for (final Stream streamDescription : streams) {
            final String referenceId = streamDescription.getId();
            final org.graylog2.plugin.streams.Stream stream = createStream(bundleId, streamDescription, userName);
            createdStreams.put(stream.getId(), stream);

            if (!isNullOrEmpty(referenceId)) {
                streamsByReferenceId.put(referenceId, stream);
            }
        }
    }

    private org.graylog2.plugin.streams.Stream createStream(final String bundleId, final Stream streamDescription, final String userName)
            throws ValidationException {
        final ImmutableMap.Builder<String, Object> streamData = ImmutableMap.builder();
        streamData.put(StreamImpl.FIELD_TITLE, streamDescription.getTitle());
        streamData.put(StreamImpl.FIELD_DESCRIPTION, streamDescription.getDescription());
        streamData.put(StreamImpl.FIELD_DISABLED, streamDescription.isDisabled());
        streamData.put(StreamImpl.FIELD_CREATOR_USER_ID, userName);
        streamData.put(StreamImpl.FIELD_CREATED_AT, Tools.iso8601());
        streamData.put(StreamImpl.FIELD_CONTENT_PACK, bundleId);

        final org.graylog2.plugin.streams.Stream stream = streamService.create(streamData.build());
        final String streamId = streamService.save(stream);

        if (streamDescription.getStreamRules() != null) {
            for (StreamRule streamRule : streamDescription.getStreamRules()) {
                final ImmutableMap.Builder<String, Object> streamRuleData = ImmutableMap.builder();
                streamRuleData.put(StreamRuleImpl.FIELD_TYPE, streamRule.getType().toInteger());
                streamRuleData.put(StreamRuleImpl.FIELD_VALUE, streamRule.getValue());
                streamRuleData.put(StreamRuleImpl.FIELD_FIELD, streamRule.getField());
                streamRuleData.put(StreamRuleImpl.FIELD_INVERTED, streamRule.isInverted());
                streamRuleData.put(StreamRuleImpl.FIELD_STREAM_ID, new ObjectId(streamId));
                streamRuleData.put(StreamRuleImpl.FIELD_CONTENT_PACK, bundleId);

                streamRuleService.save(new StreamRuleImpl(streamRuleData.build()));
            }
        }

        for (final String outputId : streamDescription.getOutputs()) {
            if (isNullOrEmpty(outputId)) {
                LOG.warn("Couldn't find referenced output <{}> for stream <{}>", outputId, streamDescription.getTitle());
            } else {
                streamService.addOutput(stream, outputsByReferenceId.get(outputId));
            }
        }

        return stream;
    }

    private void createDashboards(final String bundleId, final Set<Dashboard> dashboards, final String userName)
            throws org.graylog2.dashboards.widgets.DashboardWidget.NoSuchWidgetTypeException, InvalidWidgetConfigurationException, InvalidRangeParametersException, ValidationException {
        for (final Dashboard dashboard : dashboards) {
            org.graylog2.dashboards.Dashboard createdDashboard = createDashboard(bundleId, dashboard, userName);
            createdDashboards.put(createdDashboard.getId(), createdDashboard);
        }
    }

    private org.graylog2.dashboards.Dashboard createDashboard(final String bundleId, final Dashboard dashboardDescription, final String userName)
            throws ValidationException, org.graylog2.dashboards.widgets.DashboardWidget.NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        // Create dashboard.
        final Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put(DashboardImpl.FIELD_TITLE, dashboardDescription.getTitle());
        dashboardData.put(DashboardImpl.FIELD_DESCRIPTION, dashboardDescription.getDescription());
        dashboardData.put(DashboardImpl.FIELD_CONTENT_PACK, bundleId);
        dashboardData.put(DashboardImpl.FIELD_CREATOR_USER_ID, userName);
        dashboardData.put(DashboardImpl.FIELD_CREATED_AT, Tools.iso8601());

        final org.graylog2.dashboards.Dashboard dashboard = new DashboardImpl(dashboardData);
        final String dashboardId = dashboardService.save(dashboard);

        final ImmutableList.Builder<WidgetPositionRequest> widgetPositions = ImmutableList.builder();
        for (DashboardWidget dashboardWidget : dashboardDescription.getDashboardWidgets()) {
            final org.graylog2.dashboards.widgets.DashboardWidget widget = createDashboardWidget(dashboardWidget, userName);
            dashboardService.addWidget(dashboard, widget);

            final WidgetPositionRequest widgetPosition = new WidgetPositionRequest(widget.getId(),
                    dashboardWidget.getCol(), dashboardWidget.getRow());
            widgetPositions.add(widgetPosition);
        }

        // FML: We need to reload the dashboard because not all fields (I'm looking at you, "widgets") is set in the
        // Dashboard instance used before.
        final org.graylog2.dashboards.Dashboard persistedDashboard;
        try {
            persistedDashboard = dashboardService.load(dashboardId);
            dashboardService.updateWidgetPositions(persistedDashboard, widgetPositions.build());
        } catch (NotFoundException e) {
            LOG.error("Failed to load dashboard with id " + dashboardId, e);
        }

        dashboardRegistry.add(dashboard);

        return dashboard;
    }

    @SuppressWarnings("unchecked")
    private org.graylog2.dashboards.widgets.DashboardWidget createDashboardWidget(
            final DashboardWidget dashboardWidget, final String userName)
            throws InvalidRangeParametersException, org.graylog2.dashboards.widgets.DashboardWidget.NoSuchWidgetTypeException, InvalidWidgetConfigurationException {
        final org.graylog2.dashboards.widgets.DashboardWidget.Type type = dashboardWidget.getType();
        final Map<String, Object> config = dashboardWidget.getConfiguration();

        // Replace "stream_id" in config if it's set
        final String streamReference = (String) config.get("stream_id");
        if (!isNullOrEmpty(streamReference)) {
            final org.graylog2.plugin.streams.Stream stream = streamsByReferenceId.get(streamReference);
            if (null != stream) {
                config.put("stream_id", stream.getId());
            } else {
                LOG.warn("Couldn't find referenced stream {}", streamReference);
            }
        }

        // Build timerange.
        final Map<String, Object> timerangeConfig = (Map<String, Object>) config.get("timerange");
        final TimeRange timeRange;

        if (!timerangeConfig.containsKey("type")) {
            throw new InvalidRangeParametersException("range type not set");
        }

        final String rangeType = (String) timerangeConfig.get("type");
        switch (rangeType) {
            case "relative":
                timeRange = new RelativeRange((Integer) timerangeConfig.get("range"));
                break;
            case "keyword":
                timeRange = new KeywordRange((String) timerangeConfig.get("keyword"));
                break;
            case "absolute":
                final String from = new DateTime(timerangeConfig.get("from"), DateTimeZone.UTC).toString(Tools.ES_DATE_FORMAT);
                final String to = new DateTime(timerangeConfig.get("to"), DateTimeZone.UTC).toString(Tools.ES_DATE_FORMAT);

                timeRange = new AbsoluteRange(from, to);
                break;
            default:
                throw new InvalidRangeParametersException("range_type not recognized");
        }

        final String widgetId = UUID.randomUUID().toString();
        return org.graylog2.dashboards.widgets.DashboardWidget.buildDashboardWidget(type, metricRegistry, searches,
                widgetId, dashboardWidget.getDescription(), dashboardWidget.getCacheTime(),
                config, (String) config.get("query"), timeRange, userName);
    }
}
