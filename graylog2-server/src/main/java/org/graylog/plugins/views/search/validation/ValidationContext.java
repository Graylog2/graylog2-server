/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.validation;

import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;

import java.util.Set;

@AutoValue
public abstract class ValidationContext {

    public abstract ValidationRequest request();
    public abstract ParsedQuery query();
    public abstract Set<MappedFieldTypeDTO> availableFields();

    public static ValidationContext.Builder builder() {
        return new AutoValue_ValidationContext.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder request(ValidationRequest request);
        public abstract Builder query(ParsedQuery query);
        public abstract Builder availableFields(Set<MappedFieldTypeDTO> availableFields);

        public abstract ValidationContext build();
    }
}
