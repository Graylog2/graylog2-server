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

import lib.APIException;
import lib.Api;
import models.api.responses.GetStreamsResponse;
import models.api.responses.GetSystemJobsResponse;
import models.api.responses.SystemJobSummaryResponse;
import models.api.results.StreamsResult;
import models.api.results.SystemJobsResult;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJob {

    private final UUID id;
    private final String description;
    private final String nodeId;
    private final DateTime startedAt;
    private final User startedBy;
    private final int percentComplete;
    private final boolean isCancelable;
    private final boolean providesProgress;

    public SystemJob(SystemJobSummaryResponse s) {
        this.id = UUID.fromString(s.id);
        this.description = s.description;
        this.nodeId = s.nodeId;
        this.startedAt = DateTime.parse(s.startedAt);
        this.startedBy = null; // TODO try to load user
        this.percentComplete = s.percentComplete;
        this.isCancelable = s.isCancelable;
        this.providesProgress = s.providesProgress;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getPercentComplete() {
        return percentComplete;
    }

    public DateTime getStartedAt() {
        return startedAt;
    }

    public static SystemJobsResult all() throws IOException, APIException {
        URL url = Api.buildTarget("system/jobs");
        GetSystemJobsResponse r = Api.get(url, GetSystemJobsResponse.class);

        return new SystemJobsResult(r.jobs);
    }

}
