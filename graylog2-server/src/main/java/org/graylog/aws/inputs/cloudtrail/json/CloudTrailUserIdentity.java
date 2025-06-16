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
package org.graylog.aws.inputs.cloudtrail.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

import java.util.Map;

public class CloudTrailUserIdentity {
    private static final String USER_TYPE = "user_type";
    private static final String USER_NAME = "user_name";
    private static final String USER_PRINCIPAL_ID = "user_principal_id";
    private static final String USER_PRINCIPAL_ARN = "user_principal_arn";
    private static final String USER_ACCOUNT_ID = "user_account_id";
    @JsonProperty("type")
    public String type;
    @JsonProperty("principalId")
    public String principalId;
    @JsonProperty("arn")
    public String arn;
    @JsonProperty("accountId")
    public String accountId;
    @JsonProperty("accessKeyId")
    public String accessKeyId;
    @JsonProperty("userName")
    public String userName;
    @JsonProperty("sessionContext")
    public CloudTrailSessionContext sessionContext;

    public Map<String, Object> additionalFieldsAsMap() {
        Map<String, Object> m = Maps.newHashMap();

        m.put(USER_TYPE, type);
        m.put(USER_NAME, userName);
        m.put(USER_PRINCIPAL_ID, principalId);
        m.put(USER_PRINCIPAL_ARN, arn);
        m.put(USER_ACCOUNT_ID, accountId);
        m.put("user_access_key_id", accessKeyId);

        if (sessionContext != null && sessionContext.attributes != null) {
            m.put("user_session_creation_date", sessionContext.attributes.creationDate);
            m.put("user_session_mfa_authenticated", Boolean.valueOf(sessionContext.attributes.mfaAuthenticated));
        }

        if (sessionContext != null && sessionContext.sessionIssuer != null) {
            m.put(USER_TYPE, sessionContext.sessionIssuer.type);
            m.put(USER_NAME, sessionContext.sessionIssuer.userName);
            m.put(USER_PRINCIPAL_ID, sessionContext.sessionIssuer.principalId);
            m.put(USER_PRINCIPAL_ARN, sessionContext.sessionIssuer.arn);
            m.put(USER_ACCOUNT_ID, sessionContext.sessionIssuer.accountId);
        }

        return m;
    }

}
