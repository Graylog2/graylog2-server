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
package org.graylog2.filters.blacklist;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;

import javax.persistence.Id;

@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BlacklistIpMatcherCondition.class, name = "iprange"),
    @JsonSubTypes.Type(value = BlacklistFieldEqualityCondition.class, name = "string"),
    @JsonSubTypes.Type(value = BlacklistPatternCondition.class, name = "regex")
})
public abstract class FilterDescription {

    @Id
    @org.mongojack.ObjectId
    public ObjectId _id;

    public String creatorUserId;

    public DateTime createdAt;

    public String name;

    public String description;

    public String fieldName;

    public String pattern;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "_id=" + _id.toString() +
                ", name='" + name + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", pattern='" + pattern + '\'' +
                ", description='" + description + '\'' +
                ", creatorUserId='" + creatorUserId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
