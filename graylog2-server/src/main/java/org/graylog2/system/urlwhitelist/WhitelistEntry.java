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
package org.graylog2.system.urlwhitelist;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;

@JsonAutoDetect
@WithBeanGetter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({@Type(value = LiteralWhitelistEntry.class, name = "literal"),
        @Type(value = RegexWhitelistEntry.class, name = "regex")})
public abstract class WhitelistEntry {

    private final String id;
    private final Type type;
    private final String value;
    private final String title;

    protected WhitelistEntry(String id, Type type, String value, @Nullable String title) {
        Preconditions.checkNotNull(id, "ID of url whitelist entry must not be null");
        Preconditions.checkNotNull(type, "Type of url whitelist entry must not be null");
        Preconditions.checkNotNull(value, "Value of url whitelist entry must not be null");
        this.title = title;
        this.id = id;
        this.type = type;
        this.value = value;
    }

    enum Type {
        @JsonProperty("literal")
        LITERAL,
        @JsonProperty("regex")
        REGEX
    }

    @JsonProperty("id")
    public String id() { return id; }

    @JsonProperty("type")
    public Type type() {
        return type;
    }

    @JsonProperty("value")
    public String value() {
        return value;
    }

    @JsonProperty("title")
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    public abstract boolean isWhitelisted(String url);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final WhitelistEntry that = (WhitelistEntry) o;
        return id.equals(that.id) && type == that.type && value.equals(that.value) &&
                Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, value, title);
    }

    @Override
    public String toString() {
        return "WhitelistEntry{" + "id=" + id + ", type=" + type + ", value='" + value + '\'' + ", title='" +
                title + '\'' + '}';
    }
}
