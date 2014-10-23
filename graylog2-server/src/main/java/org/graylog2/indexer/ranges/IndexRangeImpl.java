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
package org.graylog2.indexer.ranges;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@CollectionName("index_ranges")
public class IndexRangeImpl extends PersistedImpl implements IndexRange {

    private static final Logger LOG = LoggerFactory.getLogger(IndexRangeImpl.class);

    public IndexRangeImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected IndexRangeImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public String getIndexName() {
        return (String) fields.get("index");
    }

    @Override
    public DateTime getCalculatedAt() {
        if (fields.containsKey("calculated_at")) {
            int ts = (Integer) fields.get("calculated_at");
            long unixMs = ts * 1000L;
            return new DateTime(unixMs, DateTimeZone.UTC);
        } else {
            return null;
        }
    }

    @Override
    public DateTime getStart() {
        int ts = (Integer) fields.get("start");
        long unixMs = ts * 1000L;
        return new DateTime(unixMs, DateTimeZone.UTC);
    }

    @Override
    public int getCalculationTookMs() {
        if (fields.containsKey("took_ms")) {
            return (Integer) fields.get("took_ms");
        } else {
            return -1;
        }
    }

    @Override
    public Map<String, Validator> getValidations() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

    @JsonValue
    public Map<String, Object> asMap() {
        HashMap<String, Object> fields = Maps.newHashMap();
        fields.put("index", getIndexName());
        fields.put("starts", getStart());
        // Calculated at and the calculation time in ms are not always set, depending on how/why the entry was created.
        DateTime calculatedAt = getCalculatedAt();
        if (calculatedAt != null) {
            fields.put("calculated_at", calculatedAt);
        }

        int calculationTookMs = getCalculationTookMs();
        if (calculationTookMs >= 0) {
            fields.put("calculation_took_ms", calculationTookMs);
        }
        return fields;
    }

}