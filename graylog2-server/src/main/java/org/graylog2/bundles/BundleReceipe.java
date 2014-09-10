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
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.KeywordRange;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.graylog2.inputs.InputImpl;
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
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

public class BundleReceipe {
    private static final Logger LOG = LoggerFactory.getLogger(BundleReceipe.class);

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

    @Inject
    public BundleReceipe(final InputService inputService,
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

    public void cook(final ConfigurationBundle bundle, final String userName) {
        try {
            createInputs(bundle.getInputs(), userName);
        } catch (Exception | NoSuchInputTypeException e) {
            LOG.error("Error while creating inputs but no error handling or rollback yet. Sorry.", e);
            Throwables.propagate(e);
        }

        try {
            createOutputs(bundle.getOutputs(), userName);
        } catch (Exception e) {
            LOG.error("Error while creating outputs but no error handling or rollback yet. Sorry.", e);
            Throwables.propagate(e);
        }

        try {
            createStreams(bundle.getStreams(), userName);
        } catch (Exception e) {
            LOG.error("Error while creating streams but no error handling or rollback yet. Sorry.", e);
            Throwables.propagate(e);
        }

        try {
            createDashboards(bundle.getDashboards(), userName);
        } catch (Exception e) {
            LOG.error("Error while creating dashboards but no error handling or rollback yet. Sorry.", e);
            Throwables.propagate(e);
        }
    }

    private void createInputs(final List<Input> inputs, final String userName)
            throws org.graylog2.plugin.inputs.Extractor.ReservedFieldException, org.graylog2.ConfigurationException, NoSuchInputTypeException, ValidationException, ExtractorFactory.NoSuchExtractorException, NotFoundException, ConfigurationException {
        for (final Input input : inputs) {
            final MessageInput messageInput = createMessageInput(input, userName);

            // Launch input. (this will run async and clean up itself in case of an error.)
            inputRegistry.launch(messageInput, messageInput.getId());
        }
    }

    private MessageInput createMessageInput(final Input inputDescription, final String userName)
            throws NoSuchInputTypeException, ConfigurationException, ValidationException,
            NotFoundException, org.graylog2.ConfigurationException, ExtractorFactory.NoSuchExtractorException,
            org.graylog2.plugin.inputs.Extractor.ReservedFieldException {
        final Configuration inputConfig = new Configuration(inputDescription.getConfiguration());
        final DateTime createdAt = DateTime.now(DateTimeZone.UTC);

        final MessageInput messageInput = inputRegistry.create(inputDescription.getType());
        messageInput.setTitle(inputDescription.getTitle());
        messageInput.setGlobal(inputDescription.isGlobal());
        messageInput.setCreatorUserId(userName);
        messageInput.setCreatedAt(createdAt);

        messageInput.checkConfiguration(inputConfig);

        // Don't run if exclusive and another instance is already running.
        if (messageInput.isExclusive() && inputRegistry.hasTypeRunning(messageInput.getClass())) {
            final String error = "Type is exclusive and already has input running.";
            LOG.error(error);
        }

        org.graylog2.inputs.Input mongoInput = new InputImpl(
                buildMongoDbInput(UUID.randomUUID(), inputDescription, userName, createdAt));

        // Persist input.
        final String persistId = inputService.save(mongoInput);
        messageInput.setPersistId(persistId);
        messageInput.initialize(inputConfig);

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

        messageInput.addExtractor(extractorId, extractor);

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
            final DateTime createdAt) {
        final ImmutableMap.Builder<String, Object> inputData = ImmutableMap.builder();
        inputData.put(MessageInput.FIELD_INPUT_ID, inputId);
        inputData.put(MessageInput.FIELD_TITLE, input.getTitle());
        inputData.put(MessageInput.FIELD_TYPE, input.getType());
        inputData.put(MessageInput.FIELD_CREATOR_USER_ID, userName);
        inputData.put(MessageInput.FIELD_CONFIGURATION, input.getConfiguration());
        inputData.put(MessageInput.FIELD_CREATED_AT, createdAt);

        if (input.isGlobal()) {
            inputData.put(MessageInput.FIELD_GLOBAL, true);
        } else {
            inputData.put(MessageInput.FIELD_NODE_ID, serverStatus.getNodeId().toString());
        }

        return inputData.build();
    }

    private void createOutputs(final List<Output> outputs, final String userName)
            throws ValidationException {
        for (final Output output : outputs) {
            createOutput(output, userName);
        }
    }

    private org.graylog2.plugin.streams.Output createOutput(final Output outputDescription, final String userName)
            throws ValidationException {
        return outputService.create(new OutputImpl(
                outputDescription.getTitle(),
                outputDescription.getType(),
                outputDescription.getConfiguration(),
                DateTime.now(DateTimeZone.UTC).toDate(),
                userName));
    }

    private void createStreams(final List<Stream> streams, final String userName)
            throws ValidationException {
        for (final Stream stream : streams) {
            createStream(stream, userName);
        }
    }

    private void createStream(final Stream streamDescription, final String userName)
            throws ValidationException {
        final Map<String, Object> streamData = ImmutableMap.<String, Object>of(
                "title", streamDescription.getTitle(),
                "description", streamDescription.getDescription(),
                "creator_user_id", userName,
                "created_at", DateTime.now(DateTimeZone.UTC));

        final org.graylog2.plugin.streams.Stream stream = streamService.create(streamData);
        stream.setDisabled(true);

        final String streamId = streamService.save(stream);

        if (streamDescription.getStreamRules() != null) {
            for (StreamRule streamRule : streamDescription.getStreamRules()) {
                final Map<String, Object> streamRuleData = ImmutableMap.<String, Object>of(
                        "type", streamRule.getType(),
                        "value", streamRule.getValue(),
                        "field", streamRule.getField(),
                        "inverted", streamRule.isInverted(),
                        "stream_id", new ObjectId(streamId));

                streamRuleService.save(new StreamRuleImpl(streamRuleData));
            }
        }

        // TODO Add Outputs to Streams
    }

    private void createDashboards(final List<Dashboard> dashboards, final String userName)
            throws org.graylog2.dashboards.widgets.DashboardWidget.NoSuchWidgetTypeException, InvalidWidgetConfigurationException, InvalidRangeParametersException, ValidationException {
        for (final Dashboard dashboard : dashboards) {
            createDashboard(dashboard, userName);
        }
    }

    private void createDashboard(final Dashboard dashboardDescription, final String userName)
            throws ValidationException, org.graylog2.dashboards.widgets.DashboardWidget.NoSuchWidgetTypeException, InvalidRangeParametersException, InvalidWidgetConfigurationException {
        // Create dashboard.
        Map<String, Object> dashboardData = ImmutableMap.<String, Object>of(
                "title", dashboardDescription.getTitle(),
                "description", dashboardDescription.getDescription(),
                "creator_user_id", userName,
                "created_at", DateTime.now(DateTimeZone.UTC));

        final org.graylog2.dashboards.Dashboard dashboard = new DashboardImpl(dashboardData);
        final String dashboardId = dashboardService.save(dashboard);

        final ImmutableList.Builder<WidgetPositionRequest> widgetPositions = ImmutableList.builder();
        for (DashboardWidget dashboardWidget : dashboardDescription.getDashboardWidgets()) {
            final org.graylog2.dashboards.widgets.DashboardWidget widget = createDashboardWidget(dashboardWidget, userName);
            dashboardService.addWidget(dashboard, widget);

            // Please, kill me...
            final WidgetPositionRequest positionRequest = new WidgetPositionRequest();
            positionRequest.id = widget.getId();
            positionRequest.row = dashboardWidget.getRow();
            positionRequest.col = dashboardWidget.getCol();
            widgetPositions.add(positionRequest);
        }

        dashboardService.updateWidgetPositions(dashboard, widgetPositions.build());
        dashboardRegistry.add(dashboard);
    }

    @SuppressWarnings("unchecked")
    private org.graylog2.dashboards.widgets.DashboardWidget createDashboardWidget(
            final DashboardWidget dashboardWidget, final String userName)
            throws InvalidRangeParametersException, org.graylog2.dashboards.widgets.DashboardWidget.NoSuchWidgetTypeException, InvalidWidgetConfigurationException {
        final org.graylog2.dashboards.widgets.DashboardWidget.Type type = dashboardWidget.getType();
        final Map<String, Object> config = dashboardWidget.getConfiguration();

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
                final String from = new DateTime(timerangeConfig.get("from")).toString(Tools.ES_DATE_FORMAT);
                final String to = new DateTime(timerangeConfig.get("to")).toString(Tools.ES_DATE_FORMAT);

                timeRange = new AbsoluteRange(from, to);
                break;
            default:
                throw new InvalidRangeParametersException("range_type not recognized");
        }

        final String widgetId = UUID.randomUUID().toString();
        return org.graylog2.dashboards.widgets.DashboardWidget.buildDashboardWidget(type, metricRegistry, indexer,
                widgetId, dashboardWidget.getDescription(), dashboardWidget.getCacheTime(),
                config, (String) config.get("query"), timeRange, userName);
    }
}
