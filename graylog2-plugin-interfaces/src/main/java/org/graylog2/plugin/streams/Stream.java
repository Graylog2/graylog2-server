/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.streams;

import com.fasterxml.jackson.annotation.JsonValue;
import org.graylog2.plugin.database.Persisted;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.emptyToNull;

public interface Stream extends Persisted {
    enum MatchingType {
        AND,
        OR;

        public static final MatchingType DEFAULT = AND;

        public static MatchingType valueOfOrDefault(String name) {
            return (emptyToNull(name) == null ? DEFAULT : valueOf(name));
        }
    }

    String getId();

    String getTitle();

    String getDescription();

    Boolean getDisabled();

    String getContentPack();

    void setTitle(String title);

    void setDescription(String description);

    void setDisabled(Boolean disabled);

    void setContentPack(String contentPack);

    Boolean isPaused();

    Map<String, List<String>> getAlertReceivers();

    Map<String, Object> asMap(List<StreamRule> streamRules);

    String toString();

    List<StreamRule> getStreamRules();

    Set<Output> getOutputs();

    MatchingType getMatchingType();
}
