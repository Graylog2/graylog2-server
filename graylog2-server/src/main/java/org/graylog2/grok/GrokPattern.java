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
package org.graylog2.grok;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.base.MoreObjects;
import org.bson.types.ObjectId;
import org.mongojack.Id;

import java.util.Objects;

@JsonAutoDetect
public final class GrokPattern {
    @Id
    @org.mongojack.ObjectId
    public ObjectId id;
    public String name;
    public String pattern;
    public String contentPack;

    public String name() {
        return name;
    }

    public String pattern() {
        return pattern;
    }

    public String contentPack() {
        return contentPack;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("pattern", pattern)
                .add("contentPack", contentPack)
                .toString();
    }

    public static GrokPattern create(String name, String pattern) {
        return create(null, name, pattern, null);
    }
    public static GrokPattern create(ObjectId id, String name, String pattern, String contentPack) {
        final GrokPattern grokPattern = new GrokPattern();
        grokPattern.id = id;
        grokPattern.name = name;
        grokPattern.pattern = pattern;
        grokPattern.contentPack = contentPack;
        return grokPattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final GrokPattern that = (GrokPattern) o;
        return Objects.equals(this.name, that.name) && Objects.equals(this.pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pattern);
    }
}
