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
package org.graylog2.cluster;

import org.graylog2.plugin.database.PersistedService;
import org.graylog2.plugin.system.NodeId;

import java.net.URI;
import java.util.Map;

public interface NodeService extends PersistedService {
    String registerServer(String nodeId, boolean isPrimary, URI httpPublishUri, String hostname);

    Node byNodeId(String nodeId) throws NodeNotFoundException;

    Node byNodeId(NodeId nodeId) throws NodeNotFoundException;

    Map<String, Node> allActive(Node.Type type);

    Map<String, Node> allActive();

    void dropOutdated();

    void markAsAlive(Node node, boolean isPrimary, String restTransportAddress);

    void markAsAlive(Node node, boolean isPrimary, URI restTransportAddress);

    boolean isOnlyPrimary(NodeId nodeIde);

    boolean isAnyPrimaryPresent();
}
