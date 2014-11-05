/**
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
 */
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.system.indices.*;
import org.graylog2.restroutes.generated.routes;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.graylog2.restclient.lib.Configuration.apiTimeout;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IndexService {

    private final ApiClient api;
    private final Index.Factory indexFactory;

    @Inject
    private IndexService(ApiClient api, Index.Factory indexFactory) {
        this.api = api;
        this.indexFactory = indexFactory;
    }

    public List<Index> all() throws APIException, IOException {
        List<Index> indices = Lists.newArrayList();

        IndexRangesResponse rr = api.path(routes.IndexRangesResource().list(), IndexRangesResponse.class)
                .execute();

        for (IndexRangeSummary range : rr.ranges) {
            indices.add(indexFactory.fromRangeResponse(range));
        }

        return indices;
    }

    public DeflectorInformationResponse getDeflectorInfo() throws APIException, IOException {
        return api.path(routes.DeflectorResource().deflector(), DeflectorInformationResponse.class)
                .execute();
    }

    public DeflectorConfigResponse getDeflectorConfig() throws APIException, IOException {
        return api.path(routes.DeflectorResource().config(), DeflectorConfigResponse.class)
                .onlyMasterNode()
                .execute();
    }

    public ClosedIndicesResponse getClosedIndices() throws APIException, IOException {
        return api.path(routes.IndicesResource().closed(), ClosedIndicesResponse.class)
                .execute();
    }

    public void recalculateRanges() throws APIException, IOException {
        api.path(routes.IndexRangesResource().rebuild())
                .expect(202)
                .execute();
    }

    public void cycleDeflector() throws APIException, IOException {
        api.path(routes.DeflectorResource().cycle())
                .timeout(apiTimeout("cycle_deflector", 60, TimeUnit.SECONDS))
                .onlyMasterNode()
                .execute();
    }

    // Not part an Index model instance method because opening/closing can be applied to indices without calculated ranges.
    public void close(String index) throws APIException, IOException {
        api.path(routes.IndicesResource().close(index))
                .timeout(apiTimeout("index_close", 60, TimeUnit.SECONDS))
                .expect(204)
                .execute();
    }

    // Not part an Index model instance method because opening/closing can be applied to indices without calculated ranges.
    public void reopen(String index) throws APIException, IOException {
        api.path(routes.IndicesResource().reopen(index))
                .timeout(apiTimeout("index_reopen", 60, TimeUnit.SECONDS))
                .expect(204)
                .execute();
    }

    // Not part an Index model instance method because opening/closing can be applied to indices without calculated ranges.
    public void delete(String index) throws APIException, IOException {
        api.path(routes.IndicesResource().delete(index))
                .timeout(apiTimeout("index_delete", 60, TimeUnit.SECONDS))
                .expect(204)
                .execute();
    }

}
