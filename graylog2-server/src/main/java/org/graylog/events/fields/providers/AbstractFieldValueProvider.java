package org.graylog.events.fields.providers;

import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.FieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFieldValueProvider implements FieldValueProvider {
    public interface Factory<TYPE extends FieldValueProvider> extends FieldValueProvider.Factory<TYPE> {
        @Override
        TYPE create(Config config);
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFieldValueProvider.class);

    private final Config config;

    public AbstractFieldValueProvider(Config config) {
        this.config = config;
    }

    @Override
    public FieldValue get(String fieldName, EventWithContext eventWithContext) {
        try {
            return doGet(fieldName, eventWithContext);
        } catch (Exception e) {
            LOG.error("Couldn't execute field value provider: {}", config, e);
            return FieldValue.error();
        }
    }

    protected abstract FieldValue doGet(String fieldName, EventWithContext eventWithContext);
}
