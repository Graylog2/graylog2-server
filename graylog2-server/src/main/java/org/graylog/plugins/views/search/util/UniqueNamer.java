package org.graylog.plugins.views.search.util;

/**
 * Utility class to generate unique names.
 *
 * Not threadsafe, you need to lock externally.
 */
public class UniqueNamer {

    private final String prefix;

    private long number = 0;

    public UniqueNamer() {
        this("name-");
    }

    public UniqueNamer(String prefix) {
        this.prefix = prefix;
    }

    public String nextName() {
        return prefix + ++number;
    }

    public String currentName() {
        return prefix + number;
    }

}
