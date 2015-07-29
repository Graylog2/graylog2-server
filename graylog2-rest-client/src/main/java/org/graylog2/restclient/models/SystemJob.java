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
package org.graylog2.restclient.models;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.rest.models.system.SystemJobSummary;
import org.joda.time.DateTime;

import java.util.Locale;
import java.util.UUID;

public class SystemJob {
    public interface Factory {
        SystemJob fromSummaryResponse(SystemJobSummary r);
    }

    // Some known SystemJob types that can be triggered manually from the web interface.
    public enum Type {
        FIX_DEFLECTOR_DELETE_INDEX,
        FIX_DEFLECTOR_MOVE_INDEX;

        public static Type fromString(String name) {
            return valueOf(name.toUpperCase(Locale.ENGLISH));
        }
    }

    private final UUID id;
    private final String name;
    private final String description;
    private final String info;
    private final Node node;
    private final DateTime startedAt;
    private final int percentComplete;
    private final boolean isCancelable;
    private final boolean providesProgress;

    @AssistedInject
    public SystemJob(NodeService nodeService, @Assisted SystemJobSummary s) {
        this.id = s.id();
        this.name = s.name();
        this.description = s.description();
        this.info = s.info();
        this.startedAt = s.startedAt();
        this.percentComplete = s.percentComplete();
        this.isCancelable = s.isCancelable();
        this.providesProgress = s.providesProgress();

        try {
            this.node = nodeService.loadNode(s.nodeId());
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

    public String getInfo() {
        return info;
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
