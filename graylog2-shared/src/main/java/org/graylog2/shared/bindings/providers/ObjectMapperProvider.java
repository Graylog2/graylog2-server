package org.graylog2.shared.bindings.providers;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ObjectMapperProvider implements Provider<ObjectMapper> {
    @Override
    public ObjectMapper get() {
        org.graylog2.shared.rest.ObjectMapperProvider provider = new org.graylog2.shared.rest.ObjectMapperProvider();
        return provider.getContext(null);
    }
}
