package org.graylog2.decorators;

import java.util.Optional;

public interface Decorator {
    String type();
    String field();
    Optional<String> stream();
}
