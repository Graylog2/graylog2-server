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

package models.descriptions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.models.ClusterEntity;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.Radio;

public class NodeDescription {
    @JsonIgnore
    private final ClusterEntity entity;

    public NodeDescription(ClusterEntity cluster) {
        this.entity = cluster;
    }

    @JsonProperty
    public String getNodeId() {
        return entity.getNodeId();
    }

    @JsonProperty
    public String getShortNodeId() {
        return entity.getShortNodeId();
    }

    @JsonProperty
    public String getHostname() {
        return entity.getHostname();
    }

    @JsonProperty
    public boolean isMaster() {
        return entity instanceof Node && ((Node) entity).isMaster();
    }

    @JsonProperty
    public boolean isRadio() {
        return entity instanceof Radio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeDescription that = (NodeDescription) o;

        return entity.equals(that.entity);

    }

    @Override
    public int hashCode() {
        return entity.hashCode();
    }
}
