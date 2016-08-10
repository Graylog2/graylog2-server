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
package org.graylog2.rest.resources.system.jobs;

import org.graylog2.rest.models.system.SystemJobSummary;
import org.graylog2.rest.models.system.jobs.requests.TriggerRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.List;
import java.util.Map;

public interface RemoteSystemJobResource {
    @GET("system/jobs")
    Call<Map<String, List<SystemJobSummary>>> list();

    @GET("system/jobs/{jobId}")
    Call<SystemJobSummary> get(@Path("jobId") String jobId);

    @DELETE("system/jobs/{jobId}")
    Call<SystemJobSummary> delete(@Path("jobId") String jobId);

    @POST("system/jobs")
    Call trigger(@Body TriggerRequest tr);
}
