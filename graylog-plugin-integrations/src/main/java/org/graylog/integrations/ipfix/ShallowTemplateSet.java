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
package org.graylog.integrations.ipfix;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class ShallowTemplateSet {

    public abstract ImmutableList<Record> records();

    public static ShallowTemplateSet create(ImmutableList<Record> records) {
        return new AutoValue_ShallowTemplateSet(records);
    }

    public static class Record {
        private final int templateId;
        private final byte[] recordBytes;

        public Record(int templateId, byte[] recordBytes) {
            this.templateId = templateId;
            this.recordBytes = recordBytes;
        }

        public int getTemplateId() {
            return templateId;
        }

        public byte[] getRecordBytes() {
            return recordBytes;
        }
    }
}
