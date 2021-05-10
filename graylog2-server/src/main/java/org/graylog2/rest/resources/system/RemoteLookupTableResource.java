package org.graylog2.rest.resources.system;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * An HTTP Client interface for the Lookup Table API. Is intended to be used
 * in cluster-wide operations, e.g. Cache purging.
 * See {@link org.graylog2.rest.resources.system.lookup.LookupTableResource}
 */
public interface RemoteLookupTableResource {

    /**
     * See {@link org.graylog2.rest.resources.system.lookup.LookupTableResource#performPurge(String, String)}
     */
    @POST("system/lookup/tables/{idOrName}/purge")
    Call<Void> performPurge(@Path("idOrName") String idOrName, @Query("key") String key);
}
