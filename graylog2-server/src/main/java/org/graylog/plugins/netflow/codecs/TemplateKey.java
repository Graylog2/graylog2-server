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
package org.graylog.plugins.netflow.codecs;

import com.google.common.base.MoreObjects;

import java.net.SocketAddress;
import java.util.Objects;

/**
 * The unique key for template flow ids, which is exporter source address and its obversation id (source ID)
 */
public class TemplateKey {
    private final SocketAddress remoteAddress;
    private final long sourceId;
    private final int templateId;

    /**
     * A key usable for identifying netflow exporters, when the template id is irrelevant.
     * This is used for grouping buffered packets by their exporter, because template ids are only unique across remote address and source id.
     *
     * @param remoteAddress the exporters address
     * @param sourceId the observation id
     * @return object for use as cache key
     */
    public static TemplateKey idForExporter(SocketAddress remoteAddress, long sourceId) {
        return new TemplateKey(remoteAddress, sourceId, -1);
    }

    public TemplateKey(SocketAddress remoteAddress, long sourceId, int templateId) {
        this.remoteAddress = remoteAddress;
        this.sourceId = sourceId;
        this.templateId = templateId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateKey that = (TemplateKey) o;
        return sourceId == that.sourceId &&
                templateId == that.templateId &&
                Objects.equals(remoteAddress, that.remoteAddress);
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public long getSourceId() {
        return sourceId;
    }

    public int getTemplateId() {
        return templateId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteAddress, sourceId, templateId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("remoteAddress", remoteAddress)
                .add("sourceId", sourceId)
                .add("templateId", templateId)
                .toString();
    }
}
