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
package org.graylog2.contentpacks.codecs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.ConverterEntity;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.ExtractorEntity;
import org.graylog2.contentpacks.model.entities.InputEntity;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toReferenceMap;
import static org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils.toValueMap;

public class InputCodec implements EntityCodec<InputWithExtractors> {
    private static final Logger LOG = LoggerFactory.getLogger(InputCodec.class);

    private final ObjectMapper objectMapper;
    private final InputService inputService;
    private final InputRegistry inputRegistry;
    private final MessageInputFactory messageInputFactory;
    private final ExtractorFactory extractorFactory;
    private final ConverterFactory converterFactory;
    private final ServerStatus serverStatus;

    @Inject
    public InputCodec(ObjectMapper objectMapper,
                      InputService inputService,
                      InputRegistry inputRegistry,
                      MessageInputFactory messageInputFactory,
                      ExtractorFactory extractorFactory,
                      ConverterFactory converterFactory,
                      ServerStatus serverStatus) {
        this.objectMapper = objectMapper;
        this.inputService = inputService;
        this.inputRegistry = inputRegistry;
        this.messageInputFactory = messageInputFactory;
        this.extractorFactory = extractorFactory;
        this.converterFactory = converterFactory;
        this.serverStatus = serverStatus;
    }

    @Override
    public EntityWithConstraints encode(InputWithExtractors inputWithExtractors) {
        final Input input = inputWithExtractors.input();

        // TODO: Create independent representation of entity?
        final Map<String, ValueReference> staticFields = input.getStaticFields().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, kv -> ValueReference.of(kv.getValue())));
        final ReferenceMap configuration = toReferenceMap(input.getConfiguration());
        final List<ExtractorEntity> extractors = inputWithExtractors.extractors().stream()
                .map(this::encodeExtractor)
                .collect(Collectors.toList());
        final InputEntity inputEntity = InputEntity.create(
                ValueReference.of(input.getTitle()),
                configuration,
                staticFields,
                ValueReference.of(input.getType()),
                ValueReference.of(input.isGlobal()),
                extractors);
        final JsonNode data = objectMapper.convertValue(inputEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(input.getId()))
                .type(ModelTypes.INPUT)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    private ExtractorEntity encodeExtractor(Extractor extractor) {
        final List<ConverterEntity> converters = extractor.getConverters().stream()
                .map(this::encodeConverter)
                .collect(Collectors.toList());
        return ExtractorEntity.create(
                ValueReference.of(extractor.getTitle()),
                ValueReference.of(extractor.getType()),
                ValueReference.of(extractor.getCursorStrategy()),
                ValueReference.of(extractor.getTargetField()),
                ValueReference.of(extractor.getSourceField()),
                toReferenceMap(extractor.getExtractorConfig()),
                converters,
                ValueReference.of(extractor.getConditionType()),
                ValueReference.of(extractor.getConditionValue()),
                ValueReference.of(Ints.saturatedCast(extractor.getOrder())));
    }

    private ConverterEntity encodeConverter(Converter converter) {
        return ConverterEntity.create(
                ValueReference.of(converter.getType()),
                toReferenceMap(converter.getConfig()));
    }


    @Override
    public InputWithExtractors decode(Entity entity, Map<String, ValueReference> parameters, String username) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity, parameters, username);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }


    private InputWithExtractors decodeEntityV1(EntityV1 entity, Map<String, ValueReference> parameters, String username) {
        final InputEntity inputEntity = objectMapper.convertValue(entity.data(), InputEntity.class);
        final Map<String, ValueReference> staticFields = inputEntity.staticFields();

        final MessageInput messageInput;
        try {
            messageInput = createMessageInput(
                    inputEntity.title().asString(parameters),
                    inputEntity.type().asString(parameters),
                    inputEntity.global().asBoolean(parameters),
                    toValueMap(inputEntity.configuration(), parameters),
                    username);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create input", e);
        }

        final Input input;
        try {
            input = inputService.find(messageInput.getPersistId());
        } catch (NotFoundException e) {
            throw new RuntimeException("Couldn't find persisted input", e);
        }

        try {
            addStaticFields(input, messageInput, staticFields, parameters);
        } catch (ValidationException e) {
            throw new RuntimeException("Couldn't add static fields to input", e);
        }
        final List<Extractor> extractors;
        try {
            extractors = createExtractors(input, inputEntity.extractors(), username, parameters);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create extractors", e);
        }

        return InputWithExtractors.create(input, extractors);
    }

    private MessageInput createMessageInput(
            final String title,
            final String type,
            final boolean global,
            final Map<String, Object> configuration,
            final String username)
            throws NoSuchInputTypeException, ConfigurationException, ValidationException {
        final Configuration inputConfig = new Configuration(configuration);
        final DateTime createdAt = Tools.nowUTC();

        final MessageInput messageInput = messageInputFactory.create(type, inputConfig);
        messageInput.setTitle(title);
        messageInput.setGlobal(global);
        messageInput.setCreatorUserId(username);
        messageInput.setCreatedAt(createdAt);

        messageInput.checkConfiguration();

        // Don't run if exclusive and another instance is already running.
        if (messageInput.isExclusive() && inputRegistry.hasTypeRunning(messageInput.getClass())) {
            LOG.error("Input type <{}> of input <{}> is exclusive and already has input running.",
                    messageInput.getClass(), messageInput.getTitle());
        }

        final Input mongoInput = inputService.create(buildMongoDbInput(title, type, global, configuration, username, createdAt));

        // Persist input
        final String persistId = inputService.save(mongoInput);
        messageInput.setPersistId(persistId);
        messageInput.initialize();

        return messageInput;
    }


    private List<Extractor> createExtractors(final Input input,
                                             final List<ExtractorEntity> extractorEntities,
                                             final String username,
                                             final Map<String, ValueReference> parameters)
            throws org.graylog2.plugin.inputs.Extractor.ReservedFieldException, org.graylog2.ConfigurationException,
            ExtractorFactory.NoSuchExtractorException, ValidationException {
        final ImmutableList.Builder<Extractor> result = ImmutableList.builder();
        for (ExtractorEntity extractorEntity : extractorEntities) {
            final List<Converter> converters = createConverters(extractorEntity.converters(), parameters);
            final Extractor extractor = addExtractor(
                    input,
                    extractorEntity.title().asString(parameters),
                    extractorEntity.order().asInteger(parameters),
                    extractorEntity.cursorStrategy().asEnum(parameters, Extractor.CursorStrategy.class),
                    extractorEntity.type().asEnum(parameters, Extractor.Type.class),
                    extractorEntity.sourceField().asString(parameters),
                    extractorEntity.targetField().asString(parameters),
                    toValueMap(extractorEntity.configuration(), parameters),
                    converters,
                    extractorEntity.conditionType().asEnum(parameters, Extractor.ConditionType.class),
                    extractorEntity.conditionValue().asString(parameters),
                    username);
            result.add(extractor);
        }

        return result.build();
    }

    private Extractor addExtractor(
            final Input input,
            final String title,
            final int order,
            final Extractor.CursorStrategy cursorStrategy,
            final Extractor.Type type,
            final String sourceField,
            final String targetField,
            final Map<String, Object> configuration,
            final List<Converter> converters,
            final Extractor.ConditionType conditionType,
            final String conditionValue,
            final String username)
            throws ValidationException, org.graylog2.ConfigurationException,
            ExtractorFactory.NoSuchExtractorException, Extractor.ReservedFieldException {
        final String extractorId = UUID.randomUUID().toString();
        final Extractor extractor = extractorFactory.factory(
                extractorId,
                title,
                order,
                cursorStrategy,
                type,
                sourceField,
                targetField,
                configuration,
                username,
                converters,
                conditionType,
                conditionValue);

        inputService.addExtractor(input, extractor);

        return extractor;
    }


    private List<Converter> createConverters(final List<ConverterEntity> requestedConverters,
                                             final Map<String, ValueReference> parameters) {
        final ImmutableList.Builder<Converter> converters = ImmutableList.builder();

        for (final ConverterEntity converterEntity : requestedConverters) {
            try {
                final Converter converter = converterFactory.create(
                        converterEntity.type().asEnum(parameters, Converter.Type.class),
                        toValueMap(converterEntity.configuration(), parameters));
                converters.add(converter);
            } catch (ConverterFactory.NoSuchConverterException e) {
                LOG.warn("No such converter [" + converterEntity.type() + "]. Skipping.", e);
            } catch (org.graylog2.ConfigurationException e) {
                LOG.warn("Missing configuration for [" + converterEntity.type() + "]. Skipping.", e);
            }
        }

        return converters.build();
    }

    private void addStaticFields(final Input input, final MessageInput messageInput,
                                 final Map<String, ValueReference> staticFields,
                                 final Map<String, ValueReference> parameters)
            throws ValidationException {
        for (Map.Entry<String, ValueReference> staticField : staticFields.entrySet()) {
            addStaticField(input, messageInput, staticField.getKey(), staticField.getValue().asString(parameters));
        }
    }

    private void addStaticField(final Input input,
                                final MessageInput messageInput,
                                final String key,
                                final String value)
            throws ValidationException {
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

        // Seriously, why?
        messageInput.addStaticField(key, value);
        inputService.addStaticField(input, key, value);
    }

    private Map<String, Object> buildMongoDbInput(
            final String title,
            final String type,
            final boolean global,
            final Map<String, Object> configuration,
            final String userName,
            final DateTime createdAt) {
        final ImmutableMap.Builder<String, Object> inputData = ImmutableMap.builder();
        inputData.put(MessageInput.FIELD_TITLE, title);
        inputData.put(MessageInput.FIELD_TYPE, type);
        inputData.put(MessageInput.FIELD_CREATOR_USER_ID, userName);
        inputData.put(MessageInput.FIELD_CONFIGURATION, configuration);
        inputData.put(MessageInput.FIELD_CREATED_AT, createdAt);

        if (global) {
            inputData.put(MessageInput.FIELD_GLOBAL, true);
        } else {
            inputData.put(MessageInput.FIELD_NODE_ID, serverStatus.getNodeId().toString());
        }

        return inputData.build();
    }

    @Override
    public EntityExcerpt createExcerpt(InputWithExtractors inputWithExtractors) {
        return EntityExcerpt.builder()
                .id(ModelId.of(inputWithExtractors.input().getId()))
                .type(ModelTypes.INPUT)
                .title(inputWithExtractors.input().getTitle())
                .build();
    }
}
