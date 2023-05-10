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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudTrailResponseElements{

/*
 * Remark:
 * - All JSON properties are optional.
 * - This list is incomplete.
*/  
   
    @JsonProperty("RenewRole")
    public String renewRole;

    @JsonProperty("ExitRole")
    public String exitRole;

    @JsonProperty("volumeId")
    public String volumeId;

    @JsonProperty("instanceId")
    public String instanceId;

    @JsonProperty("device")
    public String device;

    @JsonProperty("ConsoleLogin")
    public String consoleLogin;

    @JsonProperty("status")
    public String status;

    @JsonProperty("_return")
    public String returnValue;

    @JsonProperty("description")
    public String description;

    

    public Map<String, Object> additionalFieldsAsMap() {
        Map<String, Object> m = Maps.newHashMap();


       if (renewRole != null) {
           m.put("renewRole", renewRole);
        }

       if (exitRole != null) {
           m.put("exitRole", exitRole);
        }
       if (volumeId != null) {
           m.put("volumeId", volumeId);
        }
       if (instanceId != null) {
           m.put("instanceId", instanceId);
        }
       if (device != null) {
           m.put("device", device);
        }
       if (consoleLogin != null) {
           m.put("consoleLogin", consoleLogin);
        }
       if (status != null) {
           m.put("status", status);
        }
       if (returnValue != null) {
           m.put("returnValue", returnValue);
        }

        return m;
    }

}
