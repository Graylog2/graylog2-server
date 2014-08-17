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
package org.graylog2.streams;

import org.bson.types.ObjectId;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface StreamRuleService extends PersistedService {
    StreamRule load(ObjectId id) throws NotFoundException;

    List<StreamRule> loadForStream(Stream stream) throws NotFoundException;

    StreamRule create(Map<String, Object> data);
    StreamRule create(String streamid, CreateStreamRuleRequest request);

    List<StreamRule> loadForStreamId(String streamId) throws NotFoundException;
}
