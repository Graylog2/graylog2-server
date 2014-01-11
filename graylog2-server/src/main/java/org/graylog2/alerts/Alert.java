/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.alerts;

import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Alert extends Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(Alert.class);

    private static final String COLLECTION = "alerts";

    protected Alert(Core core, Map<String, Object> fields) {
        super(core, fields);
    }

    public static Alert factory(AlertCondition.CheckResult checkResult, Core core) {
        Map<String, Object> fields = Maps.newHashMap();

        if (!checkResult.isTriggered()) {
            throw new RuntimeException("Tried to create alert from not triggered alert condition result.");
        }

        fields.put("triggered_at", checkResult.getTriggeredAt());
        fields.put("condition_id", checkResult.getTriggeredCondition().getId());
        fields.put("stream_id", checkResult.getTriggeredCondition().getStream().getId());
        fields.put("description", checkResult.getResultDescription());
        fields.put("condition_parameters", checkResult.getTriggeredCondition().getParameters());

        return new Alert(core, fields);
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return Maps.newHashMap();
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

}
