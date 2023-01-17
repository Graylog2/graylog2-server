package org.graylog2.inputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueMapperConfig;
import org.graylog2.shared.inputs.MessageInputFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EncryptedValuesSupport {

    private final MessageInputFactory messageInputFactory;
    private final ObjectMapper edgeObjectMapper;
    private final ObjectMapper dbObjectMapper;

    @Inject
    public EncryptedValuesSupport(MessageInputFactory messageInputFactory, ObjectMapper objectMapper) {
        this.messageInputFactory = messageInputFactory;
        this.edgeObjectMapper = objectMapper;
        this.dbObjectMapper = objectMapper.copy();
        EncryptedValueMapperConfig.enableDatabase(dbObjectMapper);
    }

    public Map<String, Object> transformAfterReading(Map<String, Object> inputMap) {

        final String type = (String) inputMap.get(MessageInput.FIELD_TYPE);

        final Map<String, ConfigurationField> encryptedFields = messageInputFactory.getAvailableInputs().get(type).getConfigurationRequest().getFields().entrySet().stream()
                .filter(e -> e.getValue().isEncrypted()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (encryptedFields.isEmpty()) {
            return inputMap;
        }

        final Map<String, Object> transformed = new HashMap<>(inputMap);

        @SuppressWarnings("unchecked")
        final Map<String, Object> configuration = (Map<String, Object>) inputMap.get(MessageInput.FIELD_CONFIGURATION);

        final Map<String, Object> newConfiguration = new HashMap<>();
        configuration.forEach((key, value) -> {
            if (encryptedFields.containsKey(key)) {
                newConfiguration.put(key, dbObjectMapper.convertValue(value, EncryptedValue.class));
            } else {
                newConfiguration.put(key, value);
            }
        });
        transformed.put(MessageInput.FIELD_CONFIGURATION, newConfiguration);

        return transformed;
    }

    public InputCreateRequest transformInputCreateRequest(InputCreateRequest request) {

        final String type = (String) request.type();

        // TODO: guard against unknown types
        final Map<String, ConfigurationField> encryptedFields = messageInputFactory.getAvailableInputs().get(type).getConfigurationRequest().getFields().entrySet().stream()
                .filter(e -> e.getValue().isEncrypted()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (encryptedFields.isEmpty()) {
            return request;
        }

        final Map<String, Object> configuration = request.configuration();

        final Map<String, Object> newConfiguration = new HashMap<>();
        configuration.forEach((key, value) -> {
            if (encryptedFields.containsKey(key)) {
                newConfiguration.put(key, edgeObjectMapper.convertValue(value, EncryptedValue.class));
            } else {
                newConfiguration.put(key, value);
            }
        });

        return request.toBuilder().configuration(newConfiguration).build();

    }

    public Object toDbObject(EncryptedValue encryptedValue) {
        return dbObjectMapper.convertValue(encryptedValue, TypeReferences.MAP_STRING_OBJECT);
    }
}
