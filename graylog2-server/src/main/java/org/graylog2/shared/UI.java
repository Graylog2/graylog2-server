/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UI {

    private static final String HELP_DOCS = "http://docs.graylog.org/";
    private static final String HELP_COMMUNITY = "https://www.graylog.org/community-support/";
    private static final String HELP_COMMERCIAL = "https://www.graylog.com/support/";

    private static final Logger LOG = LoggerFactory.getLogger(UI.class);

    public static void exitHardWithWall(String msg) {
        exitHardWithWall(msg, new String[0]);
    }

    public static void exitHardWithWall(String msg, String... docLinks) {
        LOG.error(wallString(msg, docLinks));
        throw new IllegalStateException(msg);
    }

    public static String wallString(String msg, String... docLinks) {
        StringBuilder sb = new StringBuilder("\n");

        sb.append("\n").append(wall("#")).append("\n");

        sb.append("ERROR: ").append(msg).append("\n");

        if (docLinks != null && docLinks.length > 0) {
            sb.append("\n").append("Please see the following link(s) to help you with this error:").append("\n\n");

            for (final String docLink : docLinks) {
                sb.append("* ").append(docLink).append("\n");
            }
        }

        sb.append("\nNeed further help?").append("\n\n");
        sb.append("* Official documentation: ").append(HELP_DOCS).append("\n");
        sb.append("* Community support: ").append(HELP_COMMUNITY).append("\n");
        sb.append("* Commercial support: ").append(HELP_COMMERCIAL).append("\n");

        sb.append("\n").append("Terminating. :(").append("\n\n");
        sb.append(wall("#"));

        return sb.toString();
    }

    private static String wall(String symbol) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < 80; i++) {
            sb.append(symbol);
        }

        return sb.append("\n").toString();
    }
}
