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
package lib;

import com.google.common.collect.Maps;
import play.api.mvc.Call;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Breadcrumbs {

    // TODO this sucks and needs a proper template engine. (like play has lol)

    private final Map<String, Call> crumbs;

    public Breadcrumbs() {
        crumbs = Maps.newLinkedHashMap();
    }

    public void addCrumb(String title, Call call) {
        crumbs.put(title, call);
    }

    public String draw() {
        StringBuilder sb = new StringBuilder();

        sb.append("<ul class=\"breadcrumb\">");

        sb.append("<li><i class='icon icon-anchor'></i>&nbsp;&nbsp;</li>");

        int i = 0;
        for(Map.Entry<String, Call> crumb : crumbs.entrySet()) {
            sb.append("<li>");

            if (i != crumbs.size()-1) {
                sb.append("<a href=\"").append(crumb.getValue().url()).append("\">");
                sb.append(crumb.getKey());
                sb.append("</a>");
            } else {
                sb.append(crumb.getKey());
            }

            if (i != crumbs.size()-1) {
                sb.append("<span class=\"divider\">/</span>");
            }

            sb.append("</li>");

            i++;
        }

        sb.append("</ul>");

        return sb.toString();
    }

}
