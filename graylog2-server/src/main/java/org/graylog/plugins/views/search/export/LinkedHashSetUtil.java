package org.graylog.plugins.views.search.export;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class LinkedHashSetUtil {
    @SafeVarargs
    public static <T> LinkedHashSet<T> linkedHashSetOf(T... elements) {
        return Arrays.stream(elements).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
