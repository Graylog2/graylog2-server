package org.graylog2.bindings.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.graylog2.database.ObjectIdSerializer;
import org.graylog2.shared.rest.ObjectMapperProvider;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerObjectMapperProvider implements Provider<ObjectMapper> {
    private final ObjectMapperProvider objectMapperProvider;
    private final SimpleModule simpleModule;

    @Inject
    public ServerObjectMapperProvider(ObjectMapperProvider objectMapperProvider) {
        this.objectMapperProvider = objectMapperProvider;
        this.simpleModule = new SimpleModule() {{
            addSerializer(new ObjectIdSerializer());
        }};
    }

    @Override
    public ObjectMapper get() {
        final ObjectMapper objectMapper = objectMapperProvider.getContext(null);
        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }
}
