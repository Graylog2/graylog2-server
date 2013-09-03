/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package models;

import com.google.common.collect.Lists;
import lib.APIException;
import lib.ApiClient;
import models.api.requests.SystemJobTriggerRequest;
import models.api.responses.system.GetSystemJobsResponse;
import models.api.responses.system.SystemJobSummaryResponse;
import org.joda.time.DateTime;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJob {

    // Some known SystemJob types that can be triggered manually from the web interface.
    public enum Type {
        FIX_DEFLECTOR_DELETE_INDEX,
        FIX_DEFLECTOR_MOVE_INDEX
    }

    private final UUID id;
    private final String name;
    private final String description;
    private final Node node;
    private final DateTime startedAt;
    private final int percentComplete;
    private final boolean isCancelable;
    private final boolean providesProgress;

    public SystemJob(SystemJobSummaryResponse s) {
        this.id = UUID.fromString(s.id);
        this.name = s.name;
        this.description = s.description;
        this.node = Node.fromId(s.nodeId);
        this.startedAt = DateTime.parse(s.startedAt);
        this.percentComplete = s.percentComplete;
        this.isCancelable = s.isCancelable;
        this.providesProgress = s.providesProgress;
    }

    public static void trigger(Type type, User user) throws IOException, APIException {
        ApiClient.post()
                .path("/system/jobs")
                .body(new SystemJobTriggerRequest(type, user))
                .expect(Http.Status.ACCEPTED)
                .execute();
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

    public static List<SystemJob> all() throws IOException, APIException {
        List<SystemJob> jobs = Lists.newArrayList();

        for(Node node : Node.all()) {
            GetSystemJobsResponse r = ApiClient.get(GetSystemJobsResponse.class).node(node).path("/system/jobs").execute();

            for (SystemJobSummaryResponse job : r.jobs) {
                jobs.add(new SystemJob(job));
            }
        }

        return jobs;
    }

}
