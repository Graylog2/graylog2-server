package org.graylog.security.authzroles;

import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
public abstract class BuiltinRole {

    public static BuiltinRole create(String name, String description, Set<String> permissions) {
        return new AutoValue_BuiltinRole(name, description, permissions);
    }

    public abstract String name();
    public abstract String description();
    public abstract Set<String> permissions();
}
