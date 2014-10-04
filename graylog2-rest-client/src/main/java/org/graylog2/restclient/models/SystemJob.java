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
package org.graylog2.restclient.models;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.models.api.responses.system.SystemJobSummaryResponse;
import org.joda.time.DateTime;

import java.util.UUID;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJob {
    public interface Factory {
        SystemJob fromSummaryResponse(SystemJobSummaryResponse r);
    }

    @Inject
    private static Factory systemJobFactory;

    // Some known SystemJob types that can be triggered manually from the web interface.
    public enum Type {
        FIX_DEFLECTOR_DELETE_INDEX,
        FIX_DEFLECTOR_MOVE_INDEX;

        public static Type fromString(String name) {
            return valueOf(name.toUpperCase());
        }
    }

    private final UUID id;
    private final String name;
    private final String description;
    private final Node node;
    private final DateTime startedAt;
    private final int percentComplete;
    private final boolean isCancelable;
    private final boolean providesProgress;

    @AssistedInject
    public SystemJob(NodeService nodeService, @Assisted SystemJobSummaryResponse s) {
        this.id = UUID.fromString(s.id);
        this.name = s.name;
        this.description = s.description;
        this.startedAt = DateTime.parse(s.startedAt);
        this.percentComplete = s.percentComplete;
        this.isCancelable = s.isCancelable;
        this.providesProgress = s.providesProgress;

        try {
            this.node = nodeService.loadNode(s.nodeId);
        } catch (NodeService.NodeNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Node getNode() {
        return node;
    }

    public int getPercentComplete() {
        return percentComplete;
    }

    public DateTime getStartedAt() {
        return startedAt;
    }

}
