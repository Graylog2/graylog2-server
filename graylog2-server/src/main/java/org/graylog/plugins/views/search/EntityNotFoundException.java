package org.graylog.plugins.views.search;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String id, Class<?> entityClass) {
        this(entityClass.getSimpleName() + " with id " + id + " doesn't exist.");
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
