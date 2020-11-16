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
package org.graylog.events.notifications;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.shared.metrics.MetricUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;

@Singleton
public class EventNotificationExecutionMetrics {
    private final MetricRegistry metricRegistry;
    private final String PREFIX = "executions";

    private enum Fields {
        TOTAL,
        SUCCESSFUL,
        FAILED_OTHER,
        FAILED_TEMPORARILY,
        FAILED_PERMANENTLY,
        IN_GRACE_PERIOD
    }

    @Inject
    public EventNotificationExecutionMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    void registerEventNotification(EventNotification eventNotification, NotificationDto notification) {
        for (Fields field : Fields.values()) {
            final String name = getNameforField(eventNotification, notification, field);
            MetricUtils.safelyRegister(metricRegistry, getNameforField(eventNotification, notification, field), new Meter());
        }
    }

    void markExecution(EventNotification eventNotification, NotificationDto notification) {
        getMeterforField(eventNotification, notification, Fields.TOTAL).mark();
    }

    void markSuccess(EventNotification eventNotification, NotificationDto notification) {
        getMeterforField(eventNotification, notification, Fields.SUCCESSFUL).mark();
    }

    void markInGrace(EventNotification eventNotification, NotificationDto notification) {
        getMeterforField(eventNotification, notification, Fields.IN_GRACE_PERIOD).mark();
    }

    void markFailedTemporarily(EventNotification eventNotification, NotificationDto notification) {
        getMeterforField(eventNotification, notification, Fields.FAILED_TEMPORARILY).mark();
    }

    void markFailedPermanently(EventNotification eventNotification, NotificationDto notification) {
        getMeterforField(eventNotification, notification, Fields.FAILED_PERMANENTLY).mark();
    }

    void markFailed(EventNotification eventNotification, NotificationDto notification) {
        getMeterforField(eventNotification, notification, Fields.FAILED_OTHER).mark();
    }

    private String getNameforField(EventNotification eventNotification, NotificationDto notification, Fields field) {
        return MetricRegistry.name(eventNotification.getClass(), notification.id(), PREFIX, field.toString().toLowerCase(Locale.ROOT));
    }

    private Meter getMeterforField(EventNotification eventNotification, NotificationDto notification, Fields field) {
        return MetricUtils.getOrRegister(metricRegistry, getNameforField(eventNotification, notification, field), new Meter());
    }
}
