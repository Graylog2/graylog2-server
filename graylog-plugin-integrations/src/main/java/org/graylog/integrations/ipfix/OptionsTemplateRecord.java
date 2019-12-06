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
public abstract class OptionsTemplateRecord {

    public abstract int templateId();

    public abstract ImmutableList<InformationElement> scopeFields();

    public abstract ImmutableList<InformationElement> optionFields();

    public static Builder builder() {
        return new AutoValue_OptionsTemplateRecord.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder templateId(int templateId);

        public abstract Builder scopeFields(ImmutableList<InformationElement> scopeFields);

        public abstract ImmutableList.Builder<InformationElement> scopeFieldsBuilder();

        public abstract Builder optionFields(ImmutableList<InformationElement> optionFields);

        public abstract ImmutableList.Builder<InformationElement> optionFieldsBuilder();

        public abstract OptionsTemplateRecord build();
    }
}
