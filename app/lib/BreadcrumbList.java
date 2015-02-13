/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
package lib;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import play.api.mvc.Call;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class BreadcrumbList {

    private final Map<String, Call> crumbs;

    public BreadcrumbList() {
        crumbs = Maps.newLinkedHashMap();
    }

    public void addCrumb(String title, Call call) {
        crumbs.put(title, call);
    }

    public List<Crumb> get() {
        List<Crumb> list = Lists.newArrayList();

        int i = 0;
        for (Map.Entry<String, Call> e : crumbs.entrySet()) {
            boolean isLast = (i == crumbs.size()-1);

            // We can have crumbs without URLs that will not get a link.
            Crumb crumb;
            if (e.getValue() == null) {
                crumb = new Crumb(e.getKey(), isLast);
            } else {
                crumb = new Crumb(e.getValue().url(), e.getKey(), isLast);
            }

            list.add(crumb);
            i++;
        }

        return list;
    }

    public class Crumb {

        private final String title;
        private final String url;
        private final boolean isLast;

        public Crumb(String url, String title, boolean isLast) {
            this.url = url;
            this.title = title;
            this.isLast = isLast;
        }

        public Crumb(String title, boolean isLast) {
            this.url = null;
            this.title = title;
            this.isLast = isLast;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public  boolean isLast() {
            return isLast;
        }
    }

}
