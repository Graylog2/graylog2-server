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
package org.graylog2.bundles;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.dashboards.widgets.InvalidWidgetConfigurationException;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.grok.GrokPatternsChangedEvent;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.dashboards.requests.WidgetPositionsRequest;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.timeranges.TimeRangeFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.plugin.inputs.Extractor.Type.GROK;
import static org.graylog2.plugin.inputs.Extractor.Type.JSON;

public class BundleImporter {
    private static final Logger LOG = LoggerFactory.getLogger(BundleImporter.class);

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

    private final Map<String, org.graylog2.grok.GrokPattern> createdGrokPatterns = new HashMap<>();
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

    public void runImport(final ConfigurationBundle bundle, final String userName) {
        final String bundleId = bundle.getId();

        try {
            createGrokPatterns(bundleId, bundle.getGrokPatterns());
            createInputs(bundleId, bundle.getInputs(), userName);
            createOutputs(bundleId, bundle.getOutputs(), userName);
            createStreams(bundleId, bundle.getStreams(), userName);
            createDashboards(bundleId, bundle.getDashboards(), userName);
        } catch (Exception e) {
            LOG.error("Error while creating entities in content pack. Starting rollback.", e);
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
            deleteCreatedGrokPatterns();
        } catch (Exception e) {
            LOG.error("Error while removing grok patterns during rollback.", e);
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

    private void deleteCreatedGrokPatterns() throws NotFoundException {
        for (String grokPatternName : createdGrokPatterns.keySet()) {
            final org.graylog2.grok.GrokPattern grokPattern = grokPatternService.load(grokPatternName);

            if (grokPattern.id() != null) {
                LOG.debug("Deleting grok pattern \"{}\" from database", grokPatternName);
                grokPatternService.delete(grokPattern.id());
            } else {
                LOG.debug("Couldn't find grok pattern \"{}\" in database", grokPatternName);
            }
        }
    }

    private void deleteCreatedInputs() throws NotFoundException {
        for (Map.Entry<String, MessageInput> entry : createdInputs.entrySet()) {
            final String inputId = entry.getKey();
            final MessageInput messageInput = entry.getValue();

            LOG.debug("Terminating message input {}", inputId);
            inputRegistry.remove(messageInput);
            final org.graylog2.inputs.Input input = inputService.find(messageInput.getId());
            inputService.destroy(input);
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

            LOG.debug("Deleting dashboard {} from database", dashboardId);
            dashboardService.destroy(entry.getValue());
        }
    }

    private void createGrokPatterns(final String bundleId, final Set<GrokPattern> grokPatterns) throws ValidationException {
        for (final GrokPattern grokPattern : grokPatterns) {
            final org.graylog2.grok.GrokPattern createdGrokPattern = createGrokPattern(bundleId, grokPattern);
            createdGrokPatterns.put(grokPattern.name(), createdGrokPattern);
        }

        clusterBus.post(GrokPatternsChangedEvent.create(Collections.emptySet(), createdGrokPatterns.keySet()));
    }

    private org.graylog2.grok.GrokPattern createGrokPattern(String bundleId, GrokPattern grokPattern) throws ValidationException {
        final org.graylog2.grok.GrokPattern pattern = org.graylog2.grok.GrokPattern.create(null, grokPattern.name(), grokPattern.pattern(), bundleId);

        return grokPatternService.save(pattern);
    }

    private void createInputs(final String bundleId, final Set<Input> inputs, final String userName)
            throws org.graylog2.plugin.inputs.Extractor.ReservedFieldException, org.graylog2.ConfigurationException, NoSuchInputTypeException, ValidationException, ExtractorFactory.NoSuchExtractorException, NotFoundException, ConfigurationException {
        for (final Input input : inputs) {
            final MessageInput messageInput = createMessageInput(bundleId, input, userName);
            createdInputs.put(messageInput.getId(), messageInput);

            // Launch input. (this will run async and clean up itself in case of an error.)
            inputLauncher.launch(messageInput);
        }
    }

    private MessageInput createMessageInput(final String bundleId, final Input inputDescription, final String userName)
            throws NoSuchInputTypeException, ConfigurationException, ValidationException,
            NotFoundException, org.graylog2.ConfigurationException, ExtractorFactory.NoSuchExtractorException,
            org.graylog2.plugin.inputs.Extractor.ReservedFieldException {
        final Configuration inputConfig = new Configuration(inputDescription.getConfiguration());
        final DateTime createdAt = Tools.nowUTC();

        final MessageInput messageInput = messageInputFactory.create(inputDescription.getType(), inputConfig);
        messageInput.setTitle(inputDescription.getTitle());
        messageInput.setGlobal(inputDescription.isGlobal());
        messageInput.setCreatorUserId(userName);
        messageInput.setCreatedAt(createdAt);
        messageInput.setContentPack(bundleId);

        messageInput.checkConfiguration();

        // Don't run if exclusive and another instance is already running.
        if (messageInput.isExclusive() && inputRegistry.hasTypeRunning(messageInput.getClass())) {
            LOG.error("Input type <{}> of input <{}> is exclusive and already has input running.",
                    messageInput.getClass(), messageInput.getTitle());
        }

        final String id = inputDescription.getId();
        final org.graylog2.inputs.Input mongoInput;
        if (id == null) {
            mongoInput = inputService.create(buildMongoDbInput(inputDescription, userName, createdAt, bundleId));
        } else {
            mongoInput = inputService.create(id, buildMongoDbInput(inputDescription, userName, createdAt, bundleId));
        }

        // Persist input.
        final String persistId = inputService.save(mongoInput);
        messageInput.setPersistId(persistId);
        messageInput.initialize();

        addStaticFields(messageInput, inputDescription.getStaticFields());
        addExtractors(messageInput, inputDescription.getExtractors(), userName);

        return messageInput;
    }

    private void validateExtractor(final Extractor extractorDescription) throws ValidationException {
        if (extractorDescription.getSourceField().isEmpty()) {
            throw new ValidationException("Missing parameter source_field in extractor " + extractorDescription.getTitle());
        }

        if (extractorDescription.getType() != GROK &&
                extractorDescription.getType() != JSON &&
                extractorDescription.getTargetField().isEmpty()) {
            throw new ValidationException("Missing parameter target_field in extractor " + extractorDescription.getTitle());
        }
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
        this.validateExtractor(extractorDescription);

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
                converters.add(converterFactory.create(converter.getType(), converter.getConfiguration()));
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
            final Input input,
            final String userName,
            final DateTime createdAt,
            final String bundleId) {
        final ImmutableMap.Builder<String, Object> inputData = ImmutableMap.builder();
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
            final org.graylog2.plugin.streams.Output output = createOutput(bundleId, outputDescription, userName);
            createdOutputs.put(output.getId(), output);
        }
    }

    private org.graylog2.plugin.streams.Output createOutput(final String bundleId, final Output outputDescription, final String userName)
            throws ValidationException {
        final String referenceId = outputDescription.getId();
        final org.graylog2.plugin.streams.Output output = outputService.create(OutputImpl.create(
                outputDescription.getId(),
                outputDescription.getTitle(),
                outputDescription.getType(),
                userName,
                outputDescription.getConfiguration(),
                Tools.nowUTC().toDate(),
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

        // We cannot create streams without having a default index set.
        final IndexSet indexSet = indexSetRegistry.getDefault();

        final Map<String, Object> streamData = ImmutableMap.<String, Object>builder()
                .put(StreamImpl.FIELD_TITLE, streamDescription.getTitle())
                .put(StreamImpl.FIELD_DESCRIPTION, streamDescription.getDescription())
                .put(StreamImpl.FIELD_DISABLED, streamDescription.isDisabled())
                .put(StreamImpl.FIELD_MATCHING_TYPE, streamDescription.getMatchingType().name())
                .put(StreamImpl.FIELD_CREATOR_USER_ID, userName)
                .put(StreamImpl.FIELD_CREATED_AT, Tools.nowUTC())
                .put(StreamImpl.FIELD_CONTENT_PACK, bundleId)
                .put(StreamImpl.FIELD_DEFAULT_STREAM, streamDescription.isDefaultStream())
                .put(StreamImpl.FIELD_INDEX_SET_ID, indexSet.getConfig().id())
                .build();

        final String defaultStreamId = org.graylog2.plugin.streams.Stream.DEFAULT_STREAM_ID;
        final ObjectId id = streamDescription.isDefaultStream() ? new ObjectId(defaultStreamId) : new ObjectId(streamDescription.getId());
        final org.graylog2.plugin.streams.Stream stream = new StreamImpl(id, streamData, Collections.emptyList(), Collections.emptySet(), indexSet);

        final String streamId = streamService.save(stream);
        if (streamDescription.getStreamRules() != null) {
            for (StreamRule streamRule : streamDescription.getStreamRules()) {
                final Map<String, Object> streamRuleData = ImmutableMap.<String, Object>builder()
                        .put(StreamRuleImpl.FIELD_TYPE, streamRule.getType().toInteger())
                        .put(StreamRuleImpl.FIELD_VALUE, streamRule.getValue())
                        .put(StreamRuleImpl.FIELD_FIELD, streamRule.getField())
                        .put(StreamRuleImpl.FIELD_INVERTED, streamRule.isInverted())
                        .put(StreamRuleImpl.FIELD_STREAM_ID, new ObjectId(streamId))
                        .put(StreamRuleImpl.FIELD_CONTENT_PACK, bundleId)
                        .put(StreamRuleImpl.FIELD_DESCRIPTION, streamRule.getDescription())
                        .build();
                streamRuleService.save(new StreamRuleImpl(streamRuleData));
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
        dashboardData.put(DashboardImpl.FIELD_CREATED_AT, Tools.nowUTC());

        final org.graylog2.dashboards.Dashboard dashboard = new DashboardImpl(dashboardData);
        final String dashboardId = dashboardService.save(dashboard);

        final ImmutableList.Builder<WidgetPositionsRequest.WidgetPosition> widgetPositions = ImmutableList.builder();
        for (DashboardWidget dashboardWidget : dashboardDescription.getDashboardWidgets()) {
            final org.graylog2.dashboards.widgets.DashboardWidget widget = createDashboardWidget(dashboardWidget, userName);
            dashboardService.addWidget(dashboard, widget);

            final WidgetPositionsRequest.WidgetPosition widgetPosition = WidgetPositionsRequest.WidgetPosition.create(widget.getId(),
                    dashboardWidget.getCol(), dashboardWidget.getRow(), dashboardWidget.getHeight(), dashboardWidget.getWidth());
            widgetPositions.add(widgetPosition);
        }

        // FML: We need to reload the dashboard because not all fields (I'm looking at you, "widgets") is set in the
        // Dashboard instance used before.
        final org.graylog2.dashboards.Dashboard persistedDashboard;
        try {
            persistedDashboard = dashboardService.load(dashboardId);
            dashboardService.updateWidgetPositions(persistedDashboard, WidgetPositionsRequest.create(widgetPositions.build()));
        } catch (NotFoundException e) {
            LOG.error("Failed to load dashboard with id " + dashboardId, e);
        }

        return dashboard;
    }

    @SuppressWarnings("unchecked")
    private org.graylog2.dashboards.widgets.DashboardWidget createDashboardWidget(
            final DashboardWidget dashboardWidget, final String userName)
            throws InvalidRangeParametersException, org.graylog2.dashboards.widgets.DashboardWidget.NoSuchWidgetTypeException, InvalidWidgetConfigurationException {
        final String type = dashboardWidget.getType();
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
        final TimeRange timeRange = timeRangeFactory.create(timerangeConfig);

        final String widgetId = UUID.randomUUID().toString();
        return dashboardWidgetCreator.buildDashboardWidget(type,
                widgetId, dashboardWidget.getDescription(), dashboardWidget.getCacheTime(),
                config, timeRange, userName);
    }
}
