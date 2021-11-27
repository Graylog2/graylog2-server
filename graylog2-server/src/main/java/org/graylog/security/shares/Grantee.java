package org.graylog.security.shares;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;

@AutoValue
public abstract class Grantee {
    @JsonProperty("id")
    public abstract GRN grn();

    @JsonProperty("type")
    public abstract String type();

    @JsonProperty("title")
    public abstract String title();

    public static Grantee create(GRN grn, String type, String title) {
        return new AutoValue_Grantee(grn, type, title);
    }

    public static Grantee createGlobal() {
        return create(GRNRegistry.GLOBAL_USER_GRN, "global", "Everyone");
    }

    public static Grantee createUser(GRN grn, String title) {
        return create(grn, "user", title);
    }

}
