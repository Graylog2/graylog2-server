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

package org.graylog2.shared.rest.resources.system;

import org.graylog2.rest.models.system.responses.SystemJVMResponse;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.rest.models.system.responses.SystemProcessBufferDumpResponse;
import org.graylog2.rest.models.system.responses.SystemThreadDumpResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface RemoteSystemResource {
    @GET("system")
    Call<SystemOverviewResponse> system();

    @GET("system/jvm")
    Call<SystemJVMResponse> jvm();

    @GET("system/threaddump")
    Call<SystemThreadDumpResponse> threadDump();

    @GET("system/processbufferdump")
    Call<SystemProcessBufferDumpResponse> processBufferDump();
}
