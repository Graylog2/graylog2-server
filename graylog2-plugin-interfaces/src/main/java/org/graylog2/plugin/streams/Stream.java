/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
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

import org.graylog2.plugin.database.Persisted;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Stream extends Persisted {

    public String getId();

    public String getTitle();

    public String getDescription();

    public Boolean getDisabled();

    public String getContentPack();

    public void setTitle(String title);

    public void setDescription(String description);

    public void setDisabled(Boolean disabled);

    public void setContentPack(String contentPack);

    public Boolean isPaused();

    Map<String, List<String>> getAlertReceivers();

    public Map<String, Object> asMap(List<StreamRule> streamRules);

    public String toString();

    public List<StreamRule> getStreamRules();

    public Set<Output> getOutputs();
}
