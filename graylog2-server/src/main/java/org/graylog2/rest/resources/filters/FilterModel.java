/**
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
 */
package org.graylog2.rest.resources.filters;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonAutoDetect
public class FilterModel {

    public String creatorUserId;

    public String name;

    public String description;

    public Filters filters;

    public FilterModel() {}

    public FilterModel(String creatorUserId, String name, String description, Filters filters) {
        this.creatorUserId = creatorUserId;
        this.name = name;
        this.description = description;
        this.filters = filters;
    }

    public static class Filters {
        @JsonInclude(NON_EMPTY)
        public FieldFilter[] blacklist;

        @JsonInclude(NON_EMPTY)
        public FieldFilter[] whitelist;

        @JsonInclude(NON_EMPTY)
        public DroolsFilter[] drools;
    }

    public static class DroolsFilter {
        public String rule;

        public DroolsFilter(String rule) {
            this.rule = rule;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = IpRange.class, name = "iprange"),
        @JsonSubTypes.Type(value = FieldEquality.class, name = "string"),
        @JsonSubTypes.Type(value = FieldPattern.class, name = "regex")
    })
    public static abstract class FieldFilter {
        public String field;

        public FieldFilter() {}

        public FieldFilter(String field) {
            this.field = field;
        }

    }
    public static class IpRange extends FieldFilter {
        public String range;

        public IpRange() {}

        public IpRange(String range) {
            super("inetAddress");
            this.range = range;
        }
    }
    public static class FieldEquality extends FieldFilter {
        public String field;
        public String value;

        public FieldEquality() {}

        public FieldEquality(String field, String value) {
            super(field);
            this.value = value;
        }
    }
    public static class FieldPattern extends FieldFilter {
        public String pattern;

        public FieldPattern() {}

        public FieldPattern(String field, String pattern) {
            super(field);
            this.pattern = pattern;
        }
    }


}
