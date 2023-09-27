/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.bootstrap.preflight;

import org.graylog2.cluster.Node;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NullNotificationService implements NotificationService {
    @Override
    public Notification build() {
        return null;
    }

    @Override
    public Notification buildNow() {
        return null;
    }

    @Override
    public boolean fixed(Notification.Type type) {
        return false;
    }

    @Override
    public boolean fixed(Notification.Type type, String key) {
        return false;
    }

    @Override
    public boolean fixed(Notification.Type type, Node node) {
        return false;
    }

    @Override
    public boolean isFirst(Notification.Type type) {
        return false;
    }

    @Override
    public List<Notification> all() {
        return null;
    }

    @Override
    public Optional<Notification> getByTypeAndKey(Notification.Type type, @Nullable String key) {
        return Optional.empty();
    }

    @Override
    public boolean publishIfFirst(Notification notification) {
        return false;
    }

    @Override
    public boolean fixed(Notification notification) {
        return false;
    }

    @Override
    public int destroyAllByType(Notification.Type type) {
        return 0;
    }

    @Override
    public int destroyAllByTypeAndKey(Notification.Type type, @Nullable String key) {
        return 0;
    }

    @Override
    public <T extends Persisted> int destroy(T model) {
        return 0;
    }

    @Override
    public <T extends Persisted> int destroyAll(Class<T> modelClass) {
        return 0;
    }

    @Override
    public <T extends Persisted> String save(T model) throws ValidationException {
        return null;
    }

    @Nullable
    @Override
    public <T extends Persisted> String saveWithoutValidation(T model) {
        return null;
    }

    @Override
    public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model, Map<String, Object> fields) {
        return null;
    }

    @Override
    public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model) {
        return null;
    }

    @Override
    public Map<String, List<ValidationResult>> validate(Map<String, Validator> validators, Map<String, Object> fields) {
        return null;
    }
}
