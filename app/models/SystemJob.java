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

import java.io.IOException;
import java.net.URL;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJob {

    private final String id;
    private final String description;
    private final String nodeId;
    private final int percentComplete;

    public SystemJob(SystemJobSummaryResponse s) {
        this.id = s.id;
        this.description = s.description;
        this.nodeId = s.nodeId;
        this.percentComplete = s.percentComplete;
    }

    public String getId() {
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

    public static SystemJobsResult all() throws IOException, APIException {
        URL url = Api.buildTarget("system/jobs");
        GetSystemJobsResponse r = Api.get(url, GetSystemJobsResponse.class);

        return new SystemJobsResult(r.jobs);
    }

}
