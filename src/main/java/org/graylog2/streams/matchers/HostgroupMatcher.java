/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.streams.matchers;

import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.graylog2.hostgroups.Hostgroup;
import org.graylog2.hostgroups.HostgroupHost;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.streams.StreamRule;

/**
 * HostgroupMatcher.java: Apr 15, 2011 12:00:20 AM
 *
 * [description]
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class HostgroupMatcher implements StreamRuleMatcherIF {

    private static final Logger LOG = Logger.getLogger(HostgroupMatcher.class);

    public boolean match(GELFMessage msg, StreamRule rule) {
        ObjectId groupId = ObjectId.massageToObjectId(rule.getValue());

        Hostgroup group = null;
        try {
            group = Hostgroup.getById(groupId);
        } catch (Exception e) {
            LOG.info("Skipping hostgroup in hostgroup matcher: " + e.getMessage(), e);
            return false;
        }

         for (HostgroupHost host : group.getHosts()) {
             if (host.getType() == HostgroupHost.TYPE_SIMPLE) {
                if(msg.getHost().equals(host.getHostname())) {
                   return true;
                }
             } else if (host.getType() == HostgroupHost.TYPE_REGEX) {
                if (Pattern.compile(host.getHostname(), Pattern.DOTALL).matcher(msg.getHost()).matches()) {
                    return true;
                }
             } else {
                 LOG.info("Skipping hostgroup host in hostgroup matcher: Invalid rule type.");
             }
         }

        // Nothing matched.
         return false;
    }

}