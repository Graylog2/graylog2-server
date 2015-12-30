package org.graylog.plugins.messageprocessor;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class FieldSet {
    private static final FieldSet INSTANCE = new FieldSet(Collections.emptyMap());

    private FieldSet(Map<String, Object> map) {
        fields = map;
    }

    public static FieldSet empty() {
        return INSTANCE;
    }

    private final Map<String, Object> fields;

    public FieldSet() {
        this.fields = Maps.newHashMap();
    }

    public Object get(String key) {
        return fields.get(key);
    }

    public Object remove(String key) {
        return fields.remove(key);
    }

    public FieldSet put(String key, Object value) {
        fields.put(key, value);
        return this;
    }

    public int size() {
        return fields.size();
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    @Override
    public int hashCode() {
        return fields.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return fields.equals(o);
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return fields.entrySet();
    }

    public Set<String> keySet() {
        return fields.keySet();
    }

    public Collection<Object> values() {
        return fields.values();
    }

}
