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
package org.graylog2.rest.resources.system.logs;

import org.graylog2.rest.models.system.loggers.responses.LogMessagesSummary;
import org.graylog2.rest.models.system.loggers.responses.LoggersSummary;
import org.graylog2.rest.models.system.loggers.responses.SubsystemSummary;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RemoteLoggersResource {
    @GET("system/loggers")
    Call<LoggersSummary> loggers();

    @GET("system/loggers/subsystems")
    Call<SubsystemSummary> subsystems();

    @PUT("system/loggers/subsystems/{subsystem}/level/{level}")
    Call<Void> setSubsystemLoggerLevel(@Path("subsystem") String subsystemTitle, @Path("level") String level);

    @PUT("system/loggers/{loggerName}/level/{level}")
    Call<Void> setSingleLoggerLevel(String loggerName, String level);

    @GET("system/loggers/messages/recent")
    Call<LogMessagesSummary> messages(int limit, String level);
}
