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
package org.graylog2.restclient.models;

import org.graylog2.rest.models.system.usagestats.UsageStatsOptOutState;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restroutes.PathMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;

public class UsageStatsService {
    private static final Logger LOG = LoggerFactory.getLogger(UsageStatsService.class);
    private static final String PATH = "/plugins/org.graylog.plugins.usagestatistics/opt-out";

    private final ApiClient api;

    @Inject
    public UsageStatsService(ApiClient api) {
        this.api = api;
    }

    @Nullable
    public UsageStatsOptOutState getOptOutState() {
        try {
            return api.path(new PathMethod("GET", PATH), UsageStatsOptOutState.class).execute();
        } catch (IOException e) {
            LOG.error("Unable to load usage stats opt-out state", e);
        } catch (APIException e) {
            if (e.getHttpCode() == 404) {
                LOG.debug("Opt-out state does not exist.");
            } else {
                LOG.error("Unable to load usage stats opt-out state", e);
            }
        }

        return null;
    }

    public boolean setOptOutState(UsageStatsOptOutState optOutState) {
        try {
            api.path(new PathMethod("POST", PATH)).body(optOutState).execute();
            return true;
        } catch (APIException | IOException e) {
            LOG.error("Unable to set usage stats opt-out state", e);
            return false;
        }
    }
}
