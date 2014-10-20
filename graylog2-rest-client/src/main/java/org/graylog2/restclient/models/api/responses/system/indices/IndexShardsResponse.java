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
package org.graylog2.restclient.models.api.responses.system.indices;

import com.google.gson.annotations.SerializedName;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IndexShardsResponse {

    @SerializedName("store_size_bytes")
    public long storeSizeBytes;

    @SerializedName("open_search_contexts")
    public int openSearchContexts;

    public ShardDocumentsResponse documents;

    public ShardMeterResponse index;
    public ShardMeterResponse flush;
    public ShardMeterResponse get;
    public ShardMeterResponse merge;
    public ShardMeterResponse refresh;

    @SerializedName("search_fetch")
    public ShardMeterResponse searchFetch;

    @SerializedName("search_query")
    public ShardMeterResponse searchQuery;

    public long segments;

}
