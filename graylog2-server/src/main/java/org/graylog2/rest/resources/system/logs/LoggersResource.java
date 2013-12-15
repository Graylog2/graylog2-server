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
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "System/Loggers", description = "Internal Graylog2 loggers")
@Path("/system/loggers")
public class LoggersResource extends RestResource {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LoggersResource.class);

    private static final Map<String, Subsystem> SUBSYSTEMS = new HashMap<String, Subsystem>() {{
        put("graylog2", new Subsystem("Graylog2", "org.graylog2", "All messages from graylog2-owned systems."));
        put("indexer", new Subsystem("Indexer", "org.elasticsearch", "All messages related to indexing and searching."));
        put("authentication", new Subsystem("Authentication", "org.apache.shiro", "All user authentication messages."));
        put("sockets", new Subsystem("Sockets", "netty", "All messages related to socket communication."));
    }};

    @GET @Timed
    @ApiOperation(value = "List all loggers and their current levels")
    @Produces(MediaType.APPLICATION_JSON)
    public String loggers() {
        Map<String, Object> loggerList = Maps.newHashMap();

        Enumeration loggers = Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
        while(loggers.hasMoreElements()) {
            Logger logger = (Logger) loggers.nextElement();

            Map<String, Object> loggerInfo = Maps.newHashMap();
            loggerInfo.put("level", logger.getEffectiveLevel().toString().toLowerCase());
            loggerInfo.put("level_syslog", logger.getEffectiveLevel().getSyslogEquivalent());

            loggerList.put(logger.getName(), loggerInfo);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("loggers", loggerList);
        result.put("total", loggerList.size());

        return json(result);
    }

    @GET @Timed
    @Path("/subsystems")
    @ApiOperation(value = "List all logger subsystems and their current levels")
    @Produces(MediaType.APPLICATION_JSON)
    public String subsytems() {
        Map<String, Object> result = Maps.newHashMap();
        Map<String, Object> subsystems = Maps.newHashMap();

        for(Map.Entry<String, Subsystem> subsystem : SUBSYSTEMS.entrySet()) {
            try {
                Map<String, Object> info = Maps.newHashMap();
                info.put("title", subsystem.getValue().getTitle());
                info.put("category", subsystem.getValue().getCategory());
                info.put("description", subsystem.getValue().getDescription());

                // Get level.
                Level effectiveLevel = Logger.getLogger(subsystem.getValue().getCategory()).getEffectiveLevel();
                info.put("level", effectiveLevel.toString().toLowerCase());
                info.put("level_syslog", effectiveLevel.getSyslogEquivalent());

                subsystems.put(subsystem.getKey(), info);
            } catch(Exception e) {
                LOG.error("Error while listing logger subsystem.", e);
                continue;
            }
        }

        result.put("subsystems", subsystems);

        return json(result);
    }

    @PUT @Timed
    @ApiOperation(value = "Set the loglevel of a whole subsystem",
                  notes = "Provided level is falling back to DEBUG if it does not exist")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such subsystem.")
    })
    @Path("/subsystems/{subsystem}/level/{level}")
    public Response setSubsystemLoggerLevel(
            @ApiParam(title = "subsystem", required = true) @PathParam("subsystem") String subsystemTitle,
            @ApiParam(title = "level", required = true) @PathParam("level") String level) {
        if (!SUBSYSTEMS.containsKey(subsystemTitle)) {
            LOG.warn("No such subsystem: [{}]. Returning 404.", subsystemTitle);
            return Response.status(404).build();
        }

        Subsystem subsystem = SUBSYSTEMS.get(subsystemTitle);

        // This is never null. Worst case is a logger that does not exist.
        Logger logger = Logger.getLogger(subsystem.getCategory());

        // Setting the level falls back to DEBUG if provided level is invalid.
        Level newLevel = Level.toLevel(level.toUpperCase());
        logger.setLevel(newLevel);

        return Response.ok().build();
    }

    @PUT @Timed
    @ApiOperation(value = "Set the loglevel of a single logger",
            notes = "Provided level is falling back to DEBUG if it does not exist")
    @Path("/{loggerName}/level/{level}")
    public Response setSingleLoggerLevel(
            @ApiParam(title = "loggerName", required = true) @PathParam("loggerName") String loggerName,
            @ApiParam(title = "level", required = true) @PathParam("level") String level) {
        // This is never null. Worst case is a logger that does not exist.
        Logger logger = Logger.getLogger(loggerName);

        // Setting the level falls back to DEBUG if provided level is invalid.
        Level newLevel = Level.toLevel(level.toUpperCase());
        logger.setLevel(newLevel);

        return Response.ok().build();
    }

    private static class Subsystem {

        private final String title;
        private final String category;
        private final String description;

        public Subsystem(String title, String category, String description) {
            this.title = title;
            this.category = category;
            this.description = description;
        }

        private String getTitle() {
            return title;
        }

        private String getCategory() {
            return category;
        }

        private String getDescription() {
            return description;
        }
    }
}
