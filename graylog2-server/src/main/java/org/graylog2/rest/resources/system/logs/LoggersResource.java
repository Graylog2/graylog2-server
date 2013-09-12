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
import java.util.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Path("/system/loggers")
public class LoggersResource extends RestResource {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LoggersResource.class);

    private static final Map<String, Subsystem> SUBSYSTEMS = new HashMap<String, Subsystem>() {{
        put("graylog2", new Subsystem("Graylog2", "org.graylog2", "All messages from graylog2-owned systems."));
        put("indexer", new Subsystem("Indexer", "org.elasticsearch", "All messages related to indexing and searching."));
        put("authentication", new Subsystem("Authentication", "org.apache.shiro", "All user authentication messages."));
        put("sockets", new Subsystem("Sockets", "netty", "All messages related to socket communication."));
    }};

    @GET
    @Timed
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

    @GET
    @Timed
    @Path("/subsystems")
    @Produces(MediaType.APPLICATION_JSON)
    public String subsytems() {
        Map<String, Object> result = Maps.newHashMap();

        for(Map.Entry<String, Subsystem> subsystem : SUBSYSTEMS.entrySet()) {
            Map<String, Object> info = Maps.newHashMap();
            info.put("title", subsystem.getValue().getTitle());
            info.put("category", subsystem.getValue().getCategory());
            info.put("description", subsystem.getValue().getDescription());

            result.put(subsystem.getKey(), info);
        }

        return json(result);
    }

    @PUT
    @Path("/subsystems/{subsystem}/level/{level}")
    @Timed
    public Response setSubsystemLoggerLevel(@PathParam("subsystem") String subsystemTitle, @PathParam("level") String level) {
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

    @PUT
    @Path("/{loggerName}/level/{level}")
    @Timed
    public Response setSingleLoggerLevel(@PathParam("loggerName") String loggerName, @PathParam("level") String level) {
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
