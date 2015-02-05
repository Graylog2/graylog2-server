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

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class SearchTools {
	
	public static final Set<String> ALLOWED_DATE_HISTOGRAM_INTERVALS = ImmutableSet.of(
			"year",
			"quarter",
			"month",
			"week",
			"day",
			"hour",
			"minute"
	);

    public static boolean isAllowedDateHistogramInterval(String interval) {
    	return ALLOWED_DATE_HISTOGRAM_INTERVALS.contains(interval);
    }
    
}
