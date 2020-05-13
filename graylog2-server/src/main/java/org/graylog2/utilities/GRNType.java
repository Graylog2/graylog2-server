package org.graylog2.utilities;

import com.google.auto.value.AutoValue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

@AutoValue
public abstract class GRNType {
    public abstract String type();

    public abstract String permissionPrefix();

    public GRN toGRN(String entity) {
        return newGRNBuilder().entity(entity).build();
    }

    public GRN.Builder newGRNBuilder() {
        return GRN.builder().type(type()).permissionPrefix(permissionPrefix());
    }

    public static GRNType create(String type, String permissionPrefix) {
        checkArgument(!isNullOrEmpty(type), "type cannot be null or empty");
        checkArgument(!isNullOrEmpty(permissionPrefix), "permissionPrefix cannot be null or empty");

        return new AutoValue_GRNType(type, permissionPrefix);
    }
}
