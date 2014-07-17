package org.graylog2.shared.bindings.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.graylog2.shared.rest.RangeJsonSerializer;

import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ObjectMapperProvider implements Provider<ObjectMapper> {
    private final SimpleModule simpleModule;

    public ObjectMapperProvider() {
        simpleModule = new SimpleModule() {
            {
                addSerializer(new RangeJsonSerializer());
            }
        };
    }

    @Override
    public ObjectMapper get() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        objectMapper.registerModule(new JodaModule());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(simpleModule);

        return objectMapper;
    }
}
