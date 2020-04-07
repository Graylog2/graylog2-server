/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.freeenterprise;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class FreeLicenseInfo {
    public enum Status {
        @JsonProperty("absent")
        ABSENT,
        @JsonProperty("staged")
        STAGED,
        @JsonProperty("installed")
        INSTALLED
    }

    private static final String FIELD_LICENSE_STATUS = "license_status";
    private static final String FIELD_CLUSTER_ID = "cluster_id";

    @JsonProperty(FIELD_LICENSE_STATUS)
    public abstract Status licenseStatus();

    @JsonProperty(FIELD_CLUSTER_ID)
    public abstract String clusterId();

    public static FreeLicenseInfo absent(String clusterId) {
        return create(clusterId, Status.ABSENT);
    }

    public static FreeLicenseInfo staged(String clusterId) {
        return create(clusterId, Status.STAGED);
    }

    public static FreeLicenseInfo installed(String clusterId) {
        return create(clusterId, Status.INSTALLED);
    }

    @JsonCreator
    public static FreeLicenseInfo create(@JsonProperty(FIELD_CLUSTER_ID) String clusterId,
                                         @JsonProperty(FIELD_LICENSE_STATUS) Status licenseStatus) {
        return new AutoValue_FreeLicenseInfo(licenseStatus, clusterId);
    }
}
