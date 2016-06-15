package org.graylog2.decorators;

import java.util.Map;
import java.util.Optional;

public interface Decorator {
    String type();
    Optional<String> stream();
    Map<String, Object> config();
}
