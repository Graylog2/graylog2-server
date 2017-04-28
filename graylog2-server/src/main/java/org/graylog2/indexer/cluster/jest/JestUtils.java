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
package org.graylog2.indexer.cluster.jest;

import com.google.gson.JsonObject;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.gson.GsonUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.graylog2.indexer.gson.GsonUtils.asJsonArray;
import static org.graylog2.indexer.gson.GsonUtils.asJsonObject;
import static org.graylog2.indexer.gson.GsonUtils.asString;

public class JestUtils {
    private JestUtils() {
    }

    public static <T extends JestResult> T execute(JestClient client, Action<T> request, Supplier<String> errorMessage) {
        final T result;
        try {
            result = client.execute(request);
        } catch (IOException e) {
            throw new ElasticsearchException(errorMessage.get(), e);
        }

        if (result.isSucceeded()) {
            return result;
        } else {
            throw new ElasticsearchException(errorMessage.get(), extractErrorDetails(result.getJsonObject()));
        }
    }

    private static List<String> extractErrorDetails(JsonObject jsonObject) {
        return Optional.of(jsonObject)
                .map(json -> asJsonObject(json.get("error")))
                .map(error -> asJsonArray(error.get("root_cause")))
                .map(Iterable::spliterator)
                .map(x -> StreamSupport.stream(x, false))
                .orElse(Stream.empty())
                .map(GsonUtils::asJsonObject)
                .map(rootCause -> asString(rootCause.get("reason")))
                .collect(Collectors.toList());
    }
}
