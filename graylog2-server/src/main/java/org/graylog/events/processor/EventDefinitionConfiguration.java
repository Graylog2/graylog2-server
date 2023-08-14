package org.graylog.events.processor;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;

public class EventDefinitionConfiguration {

    @Parameter(value = "event_definition_max_event_limit", validators = PositiveIntegerValidator.class)
    private int maxEventLimit = 1000;

    public int getMaxEventLimit() {
        return maxEventLimit;
    }
}
