package org.graylog2.rest.resources.users.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.PaginatedList;
import org.graylog2.users.UserDTO;

import java.util.Collection;

@AutoValue
@JsonDeserialize(builder = UserPageListResponse.Builder.class)
public abstract class UserPageListResponse {

    private static final String FIELD_QUERY = "query";
    private static final String FIELD_PAGINATION = "pagination";
    private static final String FIELD_TOTAL = "total";
    private static final String FIELD_SORT = "sort";
    private static final String FIELD_ORDER = "order";
    private static final String FIELD_USERS = "users";
    private static final String FIELD_ADMIN_USER = "admin_user";

    @JsonProperty(FIELD_QUERY)
    public abstract String query();

    @JsonProperty(FIELD_PAGINATION)
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @JsonProperty(FIELD_TOTAL)
    public abstract long total();

    @JsonProperty(FIELD_SORT)
    public abstract String sort();

    @JsonProperty(FIELD_ORDER)
    public abstract String order();

    @JsonProperty(FIELD_USERS)
    public abstract Collection<UserDTO> users();

    @JsonProperty(FIELD_ADMIN_USER)
    public abstract UserDTO adminUser();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_UserPageListResponse.Builder();
        }

        @JsonProperty(FIELD_QUERY)
        public abstract Builder query(String query);

        @JsonProperty(FIELD_PAGINATION)
        public abstract Builder paginationInfo(PaginatedList.PaginationInfo paginationInfo);

        @JsonProperty(FIELD_TOTAL)
        public abstract Builder total(long total);

        @JsonProperty(FIELD_SORT)
        public abstract Builder sort(String sort);

        @JsonProperty(FIELD_ORDER)
        public abstract Builder order(String order);

        @JsonProperty(FIELD_USERS)
        public abstract Builder users(Collection<UserDTO> users);

        @JsonProperty(FIELD_ADMIN_USER)
        public abstract Builder adminUser(UserDTO adminUser);

        public abstract UserPageListResponse build();
    }
}
