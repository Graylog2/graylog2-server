/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class UI {

    private static final String HELP_DOCS = "http://support.torch.sh/help/kb";
    private static final String HELP_MAILING_LIST = "http://support.torch.sh/help/kb/general/forums-mailing-list";
    private static final String HELP_ISSUE_TRACKER = "http://support.torch.sh/help/kb/general/issue-trackers";
    private static final String HELP_TORCH = "http://www.torch.sh/";

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void exitHardWithWall(String msg) {
        exitHardWithWall(msg, new String[]{});
    }

    public static void exitHardWithWall(String msg, String[] docLinks) {
        StringBuilder sb = new StringBuilder("\n");

        sb.append("\n").append(wall("#")).append("\n");

        sb.append("ERROR: ").append(msg).append("\n\n");

        sb.append("Need help?").append("\n\n");
        sb.append("* Official documentation: ").append(HELP_DOCS).append("\n");
        sb.append("* Mailing list: ").append(HELP_MAILING_LIST).append("\n");
        sb.append("* Issue tracker: ").append(HELP_ISSUE_TRACKER).append("\n");
        sb.append("* Commercial support: ").append(HELP_TORCH).append("\n");

        if (docLinks != null && docLinks.length > 0) {
            sb.append("\n").append("But we also got some specific help " +
                    "pages that might help you in this case:").append("\n\n");

            for (int i = 0; i < docLinks.length; i++) {
                sb.append("* ").append(docLink(docLinks[i])).append("\n");
            }
        }

        sb.append("\n").append("Terminating. :(").append("\n\n");

        sb.append(wall("#"));

        LOG.error(sb.toString());
        System.exit(1);
    }

    private static String wall(String symbol) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < 80; i++) {
            sb.append(symbol);
        }

        return sb.append("\n").toString();
    }

    private static String docLink(String part) {
        if (!part.startsWith("/")) {
            part = "/" + part;
        }

        return "http://support.torch.sh/help/kb" + part;
    }

}
