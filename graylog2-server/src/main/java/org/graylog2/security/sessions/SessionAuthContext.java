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
package org.graylog2.security.sessions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.rest.models.system.sessions.responses.SessionResponse;

import static org.graylog2.security.sessions.SessionAuthContext.FIELD_TYPE;

/**
 * A session auth context represents any additional information that is supposed to be persisted to a user's session
 * after successful authentication. E.g. an OIDC authentication backend might need to store the ID token in the session
 * to access it later for populating the id_token_hint parameter during logout. Or a SAML authentication backend might
 * have to store the IdPs session index to establish the relationship between Graylog and IdP sessions.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = FIELD_TYPE)
public interface SessionAuthContext {
    String FIELD_TYPE = "type";

    @JsonProperty(FIELD_TYPE)
    String type();

    /**
     * Enriches the given session response with additional context-specific information.
     * <p>
     * This method allows different authentication contexts to customize the session response
     * by adding relevant data that should be exposed to clients. For example, an OIDC context
     * might add token information.
     * </p>
     *
     * @param response the original session response to enrich
     * @return the enriched session response, or the original response if no enrichment is needed
     */
    default SessionResponse enrichSessionResponse(SessionResponse response) {
        return response;
    }
}
