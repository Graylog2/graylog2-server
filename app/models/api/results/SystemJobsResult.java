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
package models.api.results;

import com.google.common.collect.Lists;
import models.Stream;
import models.SystemJob;
import models.api.responses.StreamSummaryResponse;
import models.api.responses.SystemJobSummaryResponse;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJobsResult {

    private final List<SystemJobSummaryResponse> jobs;

    public SystemJobsResult(List<SystemJobSummaryResponse> jobs) {
        this.jobs = jobs;
    }

    public List<SystemJob> getJobs() {
        List<SystemJob> jobs = Lists.newArrayList();

        for (SystemJobSummaryResponse sjsr : this.jobs) {
            jobs.add(new SystemJob(sjsr));
        }

        return jobs;
    }

}
