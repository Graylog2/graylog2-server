/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
