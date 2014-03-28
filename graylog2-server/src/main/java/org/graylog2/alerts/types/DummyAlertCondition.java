/*
 * Copyright 2012-2014 TORCH GmbH
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

package org.graylog2.alerts.types;

import org.graylog2.alerts.AlertCondition;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class DummyAlertCondition extends AlertCondition {
    final String description = "Dummy alert to test notifications";

    public DummyAlertCondition(Stream stream, String id, Type type, DateTime createdAt, String creatorUserId, Map<String, Object> parameters) {
        super(stream, id, type, createdAt, creatorUserId, parameters);
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public CheckResult runCheck(Indexer indexer) {
        return new CheckResult(true, this, this.description, Tools.iso8601());
    }

    @Override
    public List<ResultMessage> getSearchHits() {
        return null;
    }
}
