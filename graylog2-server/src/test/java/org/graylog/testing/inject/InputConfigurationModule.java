package org.graylog.testing.inject;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;

public class InputConfigurationModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(InputConfigurationBeanDeserializerModifier.class).toInstance(InputConfigurationBeanDeserializerModifier.withoutConfig());
    }
}
