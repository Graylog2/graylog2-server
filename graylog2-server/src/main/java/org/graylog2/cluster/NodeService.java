/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.cluster;

import org.graylog2.database.PersistedService;
import org.graylog2.plugin.system.NodeId;

import java.net.URI;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface NodeService extends PersistedService {
    String registerServer(String nodeId, boolean isMaster, URI restTransportUri);

    String registerRadio(String nodeId, String restTransportUri);

    Node byNodeId(String nodeId) throws NodeNotFoundException;
    Node byNodeId(NodeId nodeId) throws NodeNotFoundException;

    Map<String, Node> allActive(Node.Type type);

    Map<String, Node> allActive();

    void dropOutdated();

    void markAsAlive(Node node, boolean isMaster, String restTransportAddress);

    void markAsAlive(Node node, boolean isMaster, URI restTransportAddress);

    boolean isOnlyMaster(NodeId nodeIde);

    boolean isAnyMasterPresent();
}
