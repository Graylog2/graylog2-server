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
package org.graylog.plugins.views;

import com.github.joschi.jadconfig.Parameter;
import org.graylog2.plugin.PluginConfigBean;
import org.joda.time.Duration;

public class ViewsConfig implements PluginConfigBean {
    private static final Duration DEFAULT_MAXIMUM_AGE_FOR_SEARCHES = Duration.standardDays(4);
    private static final String PREFIX = "views_";
    private static final String MAX_SEARCH_AGE = PREFIX + "maximum_search_age";

    @Parameter(MAX_SEARCH_AGE)
    private Duration maxSearchAge = DEFAULT_MAXIMUM_AGE_FOR_SEARCHES;
}
