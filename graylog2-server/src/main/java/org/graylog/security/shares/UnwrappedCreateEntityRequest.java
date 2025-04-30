package org.graylog.security.shares;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.annotation.Nullable;

import java.util.Optional;

public class UnwrappedCreateEntityRequest<T> {
    @JsonUnwrapped
    private T entity;

    @Nullable
    @JsonProperty("share_request")
    public EntityShareRequest shareRequest;

    public T getEntity() {
        return entity;
    }

    public void setEntity(T entity) {
        this.entity = entity;
    }

    public Optional<EntityShareRequest> getShareRequest() {
        return Optional.ofNullable(shareRequest);
    }

    public void setShareRequest(@Nullable EntityShareRequest shareRequest) {
        this.shareRequest = shareRequest;
    }
}
