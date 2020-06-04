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
package org.graylog2.indexer.messages;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;

import javax.validation.constraints.NotNull;

@AutoValue
@JsonAutoDetect
public abstract class IndexingRequest {
    public abstract IndexSet indexSet();
    public abstract Message message();

    public static IndexingRequest create(@NotNull IndexSet indexSet, @NotNull Message message) {
        return new AutoValue_IndexingRequest(indexSet, message);
    }

}
