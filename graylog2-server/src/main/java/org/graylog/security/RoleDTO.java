package org.graylog.security;

import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
public abstract class RoleDTO {
    public abstract String id();

    public abstract String title();

    public abstract Set<String> permissions();

    public static RoleDTO create(String id, String title, Set<String> permissions) {
        return builder()
                .id(id)
                .title(title)
                .permissions(permissions)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_RoleDTO.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder permissions(Set<String> permissions);

        public abstract RoleDTO build();
    }
}
