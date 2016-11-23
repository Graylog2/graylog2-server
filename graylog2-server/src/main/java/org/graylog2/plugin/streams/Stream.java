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
package org.graylog2.plugin.streams;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.database.Persisted;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.emptyToNull;

public interface Stream extends Persisted {
    String DEFAULT_STREAM_ID = "000000000000000000000001";

    enum MatchingType {
        AND,
        OR;

        public static final MatchingType DEFAULT = AND;

        @JsonCreator
        public static MatchingType valueOfOrDefault(String name) {
            return (emptyToNull(name) == null ? DEFAULT : valueOf(name));
        }
    }

    String getTitle();

    String getDescription();

    Boolean getDisabled();

    String getContentPack();

    void setTitle(String title);

    void setDescription(String description);

    void setDisabled(Boolean disabled);

    void setContentPack(String contentPack);

    void setMatchingType(MatchingType matchingType);

    Boolean isPaused();

    Map<String, List<String>> getAlertReceivers();

    Map<String, Object> asMap(List<StreamRule> streamRules);

    List<StreamRule> getStreamRules();

    Set<Output> getOutputs();

    MatchingType getMatchingType();

    boolean isDefaultStream();

    void setDefaultStream(boolean defaultStream);

    boolean getRemoveMatchesFromDefaultStream();

    void setRemoveMatchesFromDefaultStream(boolean removeMatchesFromDefaultStream);

    IndexSet getIndexSet();

    String getIndexSetId();

    void setIndexSetId(String indexSetId);
}
