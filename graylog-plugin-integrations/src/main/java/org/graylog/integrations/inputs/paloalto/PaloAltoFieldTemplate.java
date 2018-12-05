package org.graylog.integrations.inputs.paloalto;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PaloAltoFieldTemplate {
    public abstract int position();

    public abstract String field();

    public abstract PaloAltoFieldType fieldType();

    public static PaloAltoFieldTemplate create(String field, int position, PaloAltoFieldType fieldType) {
        return new AutoValue_PaloAltoFieldTemplate(position, field, fieldType);
    }
}