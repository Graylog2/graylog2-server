package org.graylog2.utilities;

import java.util.Map;

public class Entry<K, V> implements Map.Entry<K, V> {
    private final K key;
    private final V value;

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        return null;
    }

    private Entry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public static <K, V> Entry<K, V> createEntry(K key, V value) {
        return new Entry<>(key, value);
    }

    public <T> Entry<K, T> withValue(T newValue) {
        return createEntry(key, newValue);
    }
}
