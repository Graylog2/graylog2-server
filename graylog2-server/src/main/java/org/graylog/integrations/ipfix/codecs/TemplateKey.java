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
package org.graylog.integrations.ipfix.codecs;

import com.google.common.base.MoreObjects;

import java.net.SocketAddress;
import java.util.Objects;

/**
 * The unique key for template flow ids, which is exporter source address and its observation domain id (source ID)
 */
public class TemplateKey {
    private final SocketAddress remoteAddress;
    private final long observationDomainId;
    private final int templateId;

    /**
     * A key usable for identifying netflow exporters, when the template id is irrelevant.
     * This is used for grouping buffered packets by their exporter, because template ids are only unique across remote address and source id.
     *
     * @param remoteAddress the exporters address
     * @param observationDomainId the observation id
     * @return object for use as cache key
     */
    public static TemplateKey idForExporter(SocketAddress remoteAddress, long observationDomainId) {
        return new TemplateKey(remoteAddress, observationDomainId, -1);
    }

    public TemplateKey(SocketAddress remoteAddress, long observationDomainId, int templateId) {
        this.remoteAddress = remoteAddress;
        this.observationDomainId = observationDomainId;
        this.templateId = templateId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateKey that = (TemplateKey) o;
        return observationDomainId == that.observationDomainId &&
               templateId == that.templateId &&
               Objects.equals(remoteAddress, that.remoteAddress);
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public long getObservationDomainId() {
        return observationDomainId;
    }

    public int getTemplateId() {
        return templateId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteAddress, observationDomainId, templateId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("remoteAddress", remoteAddress)
                          .add("observationDomainId", observationDomainId)
                          .add("templateId", templateId)
                          .toString();
    }
}
