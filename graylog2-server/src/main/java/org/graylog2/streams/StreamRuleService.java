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
package org.graylog2.streams;

import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.PersistedService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StreamRuleService extends PersistedService {
    StreamRule load(String id) throws NotFoundException;

    List<StreamRule> loadForStream(Stream stream);

    StreamRule create(Map<String, Object> data);

    StreamRule create(@Nullable String streamId, CreateStreamRuleRequest request);

    StreamRule copy(@Nullable String streamId, StreamRule streamRule);

    String save(StreamRule streamRule) throws ValidationException;

    Set<String> save(Collection<StreamRule> streamRules) throws ValidationException;

    int destroy(StreamRule streamRule);

    List<StreamRule> loadForStreamId(String streamId);

    Map<String, List<StreamRule>> loadForStreamIds(Collection<String> streamIds);

    /**
     * @return the total number of stream rules
     */
    long totalStreamRuleCount();

    /**
     * @param streamId the stream ID
     * @return the number of stream rules for the specified stream
     */
    long streamRuleCount(String streamId);

    /**
     * @return the number of stream rules grouped by stream
     */
    Map<String, Long> streamRuleCountByStream();
}
