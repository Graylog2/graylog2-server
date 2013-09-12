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
package org.graylog2.rest.resources.system.logs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Enumeration;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/loggers")
public class LoggersResource extends RestResource {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LoggersResource.class);

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String loggers() {
        Map<String, Object> result = Maps.newHashMap();

        Enumeration loggers = Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
        while(loggers.hasMoreElements()) {
            Logger logger = (Logger) loggers.nextElement();

            Map<String, Object> loggerInfo = Maps.newHashMap();
            loggerInfo.put("level", logger.getEffectiveLevel().toString().toLowerCase());
            loggerInfo.put("level_syslog", logger.getEffectiveLevel().getSyslogEquivalent());

            result.put(logger.getName(), loggerInfo);
        }

        return json(result);
    }

    @PUT
    @Path("/{loggerName}/level/{level}")
    @Timed
    public Response setLoggerLevel(@PathParam("loggerName") String loggerName, @PathParam("level") String level) {
        // This is never null. Worst case is a logger that does not exist.
        Logger logger = Logger.getLogger(loggerName);

        // Setting the level falls back to DEBUG if provided level is invalid.
        Level newLevel = Level.toLevel(level.toUpperCase());
        logger.setLevel(newLevel);

        return Response.ok().build();
    }

}
