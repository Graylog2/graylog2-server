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
import models.api.responses.system.loggers.LoggerSummary;
import models.api.responses.system.loggers.LoggersResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class InternalLogger {

    private final String title;
    private final String level;
    private final int syslogLevel;

    public InternalLogger(String title, String level, int syslogLevel) {
        this.title = title;
        this.level = level;
        this.syslogLevel = syslogLevel;
    }

    public static List<InternalLogger> all(Node node) throws APIException, IOException {
        List<InternalLogger> loggers = Lists.newArrayList();

        LoggersResponse response = ApiClient.get(LoggersResponse.class)
                .node(node)
                .path("/system/loggers")
                .execute();

        for(Map.Entry<String, LoggerSummary> logger : response.loggers.entrySet()) {
            loggers.add(new InternalLogger(logger.getKey(), logger.getValue().level, logger.getValue().syslogLevel));
        }

        return loggers;
    }

    public String getTitle() {
        return title;
    }

    public String getLevel() {
        return level;
    }

    public int getSyslogLevel() {
        return syslogLevel;
    }

}
