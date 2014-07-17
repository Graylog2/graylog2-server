package org.graylog2.bindings.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.graylog2.database.ObjectIdSerializer;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerObjectMapperProvider extends ObjectMapperProvider implements Provider<ObjectMapper> {
    private final SimpleModule simpleModule;

    public ServerObjectMapperProvider() {
        this.simpleModule = new SimpleModule() {{
            addSerializer(new ObjectIdSerializer());
        }};
    }

    @Override
    public ObjectMapper get() {
        final ObjectMapper objectMapper = super.get();
        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }
}
