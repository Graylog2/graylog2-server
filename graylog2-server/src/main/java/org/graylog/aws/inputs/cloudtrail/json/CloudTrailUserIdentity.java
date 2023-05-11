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

        m.put("user_type", type);
        m.put("user_name", userName);
        m.put("user_principal_id", principalId);
        m.put("user_principal_arn", arn);
        m.put("user_account_id", accountId);
        m.put("user_access_key_id", accessKeyId);

        if (sessionContext != null && sessionContext.attributes != null) {
            m.put("user_session_creation_date", sessionContext.attributes.creationDate);
            m.put("user_session_mfa_authenticated", Boolean.valueOf(sessionContext.attributes.mfaAuthenticated));
        }

        return m;
    }

}
