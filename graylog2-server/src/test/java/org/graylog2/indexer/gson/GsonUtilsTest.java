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
package org.graylog2.indexer.gson;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonUtilsTest {
    @Test
    public void asJsonObject() throws Exception {
        final JsonObject jsonObject = new JsonObject();
        assertThat(GsonUtils.asJsonObject(jsonObject)).isEqualTo(jsonObject);
    }

    @Test
    public void asJsonObjectWithNull() throws Exception {
        assertThat(GsonUtils.asJsonObject(null)).isNull();
    }

    @Test
    public void asJsonObjectWithIncorrectType() throws Exception {
        final JsonPrimitive jsonPrimitive = new JsonPrimitive("test");
        assertThat(GsonUtils.asJsonObject(jsonPrimitive)).isNull();
    }

    @Test
    public void asJsonArray() throws Exception {
        final JsonArray jsonArray = new JsonArray();
        assertThat(GsonUtils.asJsonArray(jsonArray)).isEqualTo(jsonArray);
    }

    @Test
    public void asJsonArrayWithNull() throws Exception {
        assertThat(GsonUtils.asJsonArray(null)).isNull();
    }

    @Test
    public void asJsonArrayWithIncorrectType() throws Exception {
        final JsonPrimitive jsonPrimitive = new JsonPrimitive("test");
        assertThat(GsonUtils.asJsonArray(jsonPrimitive)).isNull();
    }

    @Test
    public void asString() throws Exception {
        final JsonPrimitive jsonPrimitive = new JsonPrimitive("test");
        assertThat(GsonUtils.asString(jsonPrimitive)).isEqualTo("test");
    }

    @Test
    public void asStringObjectWithNull() throws Exception {
        assertThat(GsonUtils.asString(null)).isNull();
    }

    @Test
    public void asStringWithIncorrectType() throws Exception {
        final JsonObject jsonObject = new JsonObject();
        assertThat(GsonUtils.asString(jsonObject)).isNull();
    }

    @Test
    public void asBoolean() throws Exception {
        final JsonPrimitive jsonPrimitive = new JsonPrimitive(false);
        assertThat(GsonUtils.asBoolean(jsonPrimitive)).isFalse();
    }

    @Test
    public void asBooleanWithNull() throws Exception {
        assertThat(GsonUtils.asBoolean(null)).isNull();
    }

    @Test
    public void asBooleanWithIncorrectType() throws Exception {
        final JsonPrimitive jsonPrimitive = new JsonPrimitive("test");
        assertThat(GsonUtils.asBoolean(jsonPrimitive)).isNull();
    }

    @Test
    public void asLong() throws Exception {
        final JsonPrimitive jsonPrimitive = new JsonPrimitive(42L);
        assertThat(GsonUtils.asLong(jsonPrimitive)).isEqualTo(42L);
    }

    @Test
    public void asLongWithNull() throws Exception {
        assertThat(GsonUtils.asLong(null)).isNull();
    }

    @Test
    public void asLongWithIncorrectType() throws Exception {
        final JsonPrimitive jsonPrimitive = new JsonPrimitive("test");
        assertThat(GsonUtils.asLong(jsonPrimitive)).isNull();
    }

    @Test
    public void asInteger() throws Exception {
        final JsonPrimitive jsonPrimitive = new JsonPrimitive(42);
        assertThat(GsonUtils.asInteger(jsonPrimitive)).isEqualTo(42);
    }

    @Test
    public void asIntegerWithNull() throws Exception {
        assertThat(GsonUtils.asInteger(null)).isNull();
    }

    @Test
    public void asIntegerWithIncorrectType() throws Exception {
        final JsonPrimitive jsonPrimitive = new JsonPrimitive("test");
        assertThat(GsonUtils.asInteger(jsonPrimitive)).isNull();
    }

    @Test
    public void entrySetAsMap() throws Exception {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("foo", "bar");
        jsonObject.addProperty("question", 42);

        assertThat(GsonUtils.entrySetAsMap(jsonObject))
                .containsEntry("foo", new JsonPrimitive("bar"))
                .containsEntry("question", new JsonPrimitive(42));
    }

    @Test
    public void entrySetAsMapWithNull() throws Exception {
        assertThat(GsonUtils.entrySetAsMap(null)).isNull();
    }

    @Test
    public void asMap() throws Exception {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("foo", "bar");
        jsonObject.addProperty("question", 42L);

        assertThat(GsonUtils.asMap(new Gson(), jsonObject))
                .containsEntry("foo", "bar")
                .containsEntry("question", 42.0D);
    }

    @Test
    public void asMapWithNull() throws Exception {
        assertThat(GsonUtils.asMap(new Gson(), null)).isNull();
    }
}