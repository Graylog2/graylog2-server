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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StreamMock implements Stream {
    private String id;
    private String title;
    private String description;
    private boolean disabled;
    private String contentPack;
    private List<StreamRule> streamRules;
    private MatchingType matchingType;
    private boolean defaultStream;
    private boolean removeFromAllMessages;

    public StreamMock(Map<String, Object> stream) {
        this(stream, Collections.emptyList());
    }

    public StreamMock(Map<String, Object> stream, List<StreamRule> streamRules) {
        this.id = stream.get("_id").toString();
        this.title = (String) stream.get(StreamImpl.FIELD_TITLE);
        this.description = (String) stream.get(StreamImpl.FIELD_DESCRIPTION);
        if (stream.containsKey(StreamImpl.FIELD_DISABLED)) {
            this.disabled = (boolean) stream.get(StreamImpl.FIELD_DISABLED);
        }
        this.contentPack = (String) stream.get(StreamImpl.FIELD_CONTENT_PACK);
        this.streamRules = streamRules;
        this.matchingType = (MatchingType) stream.getOrDefault(StreamImpl.FIELD_MATCHING_TYPE, MatchingType.AND);
        this.defaultStream = (boolean) stream.getOrDefault(StreamImpl.FIELD_DEFAULT_STREAM, false);
        this.removeFromAllMessages = (boolean) stream.getOrDefault(StreamImpl.FIELD_REMOVE_FROM_ALL_MESSAGES, false);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, Object> getFields() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Validator> getValidations() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> asMap() {
        return Collections.emptyMap();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Boolean getDisabled() {
        return disabled;
    }

    @Override
    public String getContentPack() {
        return contentPack;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public void setContentPack(String contentPack) {
        this.contentPack = contentPack;
    }

    @Override
    public Boolean isPaused() {
        return getDisabled() != null ? getDisabled() : false;
    }

    @Override
    public Map<String, List<String>> getAlertReceivers() {
        return Maps.newHashMap();
    }

    @Override
    public Map<String, Object> asMap(List<StreamRule> streamRules) {
        return Maps.newHashMap();
    }

    @Override
    public List<StreamRule> getStreamRules() {
        return streamRules;
    }

    public void setStreamRules(List<StreamRule> streamRules) {
        this.streamRules = streamRules;
    }

    @Override
    public Set<Output> getOutputs() {
        return Sets.newHashSet();
    }


    @Override
    public MatchingType getMatchingType() {
        return this.matchingType;
    }

    @Override
    public void setMatchingType(MatchingType matchingType) {
        this.matchingType = matchingType;
    }

    @Override
    public boolean isDefaultStream() {
        return defaultStream;
    }

    @Override
    public void setDefaultStream(boolean defaultStream) {
        this.defaultStream = defaultStream;
    }

    @Override
    public boolean getRemoveFromAllMessages() {
        return removeFromAllMessages;
    }

    @Override
    public void setRemoveFromAllMessages(boolean removeFromAllMessages) {
        this.removeFromAllMessages = removeFromAllMessages;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(StreamMock.class)
                .add("id", id)
                .add("title", title)
                .add("matchingType", matchingType)
                .add("defaultStream", defaultStream)
                .add("disabled", disabled)
                .add("removeFromAllMessages", removeFromAllMessages)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamMock that = (StreamMock) o;
        return defaultStream == that.defaultStream &&
                Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(streamRules, that.streamRules) &&
                Objects.equals(defaultStream, that.defaultStream) &&
                Objects.equals(removeFromAllMessages, that.removeFromAllMessages) &&
                matchingType == that.matchingType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, streamRules, matchingType, defaultStream, removeFromAllMessages);
    }

    @Override
    public Set<IndexSet> getIndexSets() {
        return Collections.emptySet();
    }
}
