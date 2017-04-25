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

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public final class GsonUtils {
    private GsonUtils() {
    }

    @Nullable
    public static JsonObject asJsonObject(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;
    }

    @Nullable
    public static JsonArray asJsonArray(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonArray() ? jsonElement.getAsJsonArray() : null;
    }

    @Nullable
    public static String asString(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonPrimitive() ? jsonElement.getAsString() : null;
    }

    @Nullable
    public static Boolean asBoolean(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isBoolean() ? jsonElement.getAsBoolean() : null;
    }

    @Nullable
    public static Long asLong(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber() ? jsonElement.getAsLong() : null;
    }

    @Nullable
    public static Integer asInteger(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber() ? jsonElement.getAsInt() : null;
    }

    public static Map<String, JsonElement> entrySetAsMap(JsonObject jsonObject) {
        final ImmutableMap.Builder<String, JsonElement> mapBuilder = ImmutableMap.builder();
        if (jsonObject != null) {
            final Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
            for (Map.Entry<String, JsonElement> entry : entrySet) {
                mapBuilder.put(entry.getKey(), entry.getValue());
            }
        }
        return mapBuilder.build();
    }
}
