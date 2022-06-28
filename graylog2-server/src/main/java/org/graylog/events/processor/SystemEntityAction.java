package org.graylog.events.processor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SystemEntityAction {
    EDIT,
    DELETE,
    VIEW,
    LIST,
    EXPORT;

    public static List<SystemEntityAction> all() {
        return Stream.of(values())
                .collect(Collectors.toList());
    }
}
