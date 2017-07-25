package org.graylog.plugins.netflow.v9;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class NetFlowV9FieldTypeRegistry {
    private final Map<Integer, NetFlowV9FieldType> fieldTypes;

    public NetFlowV9FieldTypeRegistry() {
        final URL url = Resources.getResource(this.getClass(), "/netflow9.csv");
        final List<String> lines;
        try {
            lines = Resources.readLines(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();
        final ImmutableMap.Builder<Integer, NetFlowV9FieldType> fieldTypesBuilder = ImmutableMap.builder();
        for (String line : lines) {
            final List<String> items = splitter.splitToList(line);
            final int id = Integer.parseInt(items.get(0));
            final String name = items.get(1);
            final NetFlowV9FieldType.ValueType type = NetFlowV9FieldType.ValueType.valueOf(items.get(2));

            fieldTypesBuilder.put(id, NetFlowV9FieldType.create(id, type, name));
        }
        this.fieldTypes = fieldTypesBuilder.build();
    }

    public NetFlowV9FieldType get(int id) {
        return fieldTypes.get(id);
    }

    public Map<Integer, NetFlowV9FieldType> asMap() {
        return fieldTypes;
    }
}
