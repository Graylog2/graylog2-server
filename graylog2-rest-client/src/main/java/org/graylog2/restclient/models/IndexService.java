/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
        api.path(routes.IndicesResource().single(index))
                .timeout(apiTimeout("index_delete", 60, TimeUnit.SECONDS))
                .expect(204)
                .execute();
    }

}
