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
package org.graylog.integrations.inputs.paloalto;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PaloAltoFieldTemplate implements Comparable<PaloAltoFieldTemplate> {
    public abstract int position();

    public abstract String field();

    public abstract PaloAltoFieldType fieldType();

    public static PaloAltoFieldTemplate create(String field, int position, PaloAltoFieldType fieldType) {
        return new AutoValue_PaloAltoFieldTemplate(position, field, fieldType);
    }

    @Override
    public int compareTo(PaloAltoFieldTemplate other) {
        return position() - other.position();
    }
}