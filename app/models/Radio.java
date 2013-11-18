/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package models;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import lib.APIException;
import lib.ApiClient;
import models.api.responses.SystemOverviewResponse;
import models.api.responses.cluster.RadioSummaryResponse;
import models.api.responses.system.ClusterEntityJVMStatsResponse;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Radio extends ClusterEntity {

    public interface Factory {
        Radio fromSummaryResponse(RadioSummaryResponse r);
    }

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Radio.class);
    private final ApiClient api;

    private final Input.Factory inputFactory;

    private final URI transportAddress;
    private DateTime lastSeen;
    private String id;
    private String shortNodeId;

    private NodeJVMStats jvmInfo;
    private SystemOverviewResponse systemInfo;

    @AssistedInject
    public Radio(ApiClient api, Input.Factory inputFactory, @Assisted RadioSummaryResponse r) {
        this.api = api;
        this.inputFactory = inputFactory;

        transportAddress = normalizeUriPath(r.transportAddress);
        lastSeen = new DateTime(r.lastSeen);
        id = r.id;
        shortNodeId = r.shortNodeId;
    }

    public synchronized void loadSystemInformation() {
        try {
            systemInfo = api.get(SystemOverviewResponse.class)
                    .path("/system")
                    .radio(this)
                    .execute();
        } catch (APIException e) {
            log.error("Unable to load system information for radio " + this, e);
        } catch (IOException e) {
            log.error("Unable to load system information for radio " + this, e);
        }
    }

    public synchronized void loadJVMInformation() {
        try {
            jvmInfo = new NodeJVMStats(api.get(ClusterEntityJVMStatsResponse.class)
                    .path("/system/jvm")
                    .radio(this)
                    .execute());
        } catch (APIException e) {
            log.error("Unable to load JVM information for radio " + this, e);
        } catch (IOException e) {
            log.error("Unable to load JVM information for radio " + this, e);
        }
    }

    public String getShortNodeId() {
        return shortNodeId;
    }

    public String getId() {
        return id;
    }

    public DateTime getLastSeen() {
        return lastSeen;
    }

    public NodeJVMStats jvm() {
        if (jvmInfo == null) {
            loadJVMInformation();
        }

        return jvmInfo;
    }

    @Override
    public String getTransportAddress() {
        return transportAddress.toASCIIString();
    }

    public String getHostname() {
        if (systemInfo == null) {
            loadSystemInformation();
        }
        return systemInfo.hostname;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        if (id == null) {
            b.append("UnresolvedNode {'").append(transportAddress).append("'}");
            return b.toString();
        }

        b.append("Node {");
        b.append("'").append(id).append("'");
        b.append(", ").append(transportAddress);
        b.append("}");
        return b.toString();
    }

}
