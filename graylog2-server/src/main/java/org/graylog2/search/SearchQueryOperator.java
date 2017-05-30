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
package org.graylog2.search;

import org.mongojack.DBQuery;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public abstract class SearchQueryOperator {
    public abstract DBQuery.Query buildQuery(String key, Object value);

    @Override
    public boolean equals(Object obj) {
        return obj.getClass().equals(getClass());
    }

    public static class Equals extends SearchQueryOperator {
        @Override
        public DBQuery.Query buildQuery(String key, Object value) {
            return DBQuery.is(key, value);
        }
    }

    public static class Regexp extends SearchQueryOperator {
        @Override
        public DBQuery.Query buildQuery(String key, Object value) {
            return DBQuery.regex(key, Pattern.compile(value.toString(), CASE_INSENSITIVE));
        }
    }

    public static class Greater extends SearchQueryOperator {
        @Override
        public DBQuery.Query buildQuery(String key, Object value) {
            return DBQuery.greaterThan(key, value);
        }
    }

    public static class GreaterEquals extends SearchQueryOperator {
        @Override
        public DBQuery.Query buildQuery(String key, Object value) {
            return DBQuery.greaterThanEquals(key, value);
        }
    }

    public static class Less extends SearchQueryOperator {
        @Override
        public DBQuery.Query buildQuery(String key, Object value) {
            return DBQuery.lessThan(key, value);
        }
    }

    public static class LessEquals extends SearchQueryOperator {
        @Override
        public DBQuery.Query buildQuery(String key, Object value) {
            return DBQuery.lessThanEquals(key, value);
        }
    }
}
