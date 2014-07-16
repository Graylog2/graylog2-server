/*
 * Copyright 2013-2014 TORCH GmbH
 *
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

package org.graylog2.plugin.streams;

import org.graylog2.plugin.database.Persisted;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public interface Stream extends Persisted {

    public String getId();

    public String getTitle();
    public String getDescription();
    public Boolean getDisabled();

    public void setTitle(String title);
    public void setDescription(String description);
    public void setDisabled(Boolean disabled);

    public Boolean isPaused();

    Map<String, List<String>> getAlertReceivers();

    public Map<String, Object> asMap(List<StreamRule> streamRules);

    public String toString();

    public List<StreamRule> getStreamRules();

    public Set<Output> getOutputs();
}
