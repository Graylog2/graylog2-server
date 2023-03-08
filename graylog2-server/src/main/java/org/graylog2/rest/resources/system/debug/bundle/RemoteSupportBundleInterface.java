package org.graylog2.rest.resources.system.debug.bundle;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

import javax.ws.rs.core.Response;

public interface RemoteSupportBundleInterface {
    @GET("system/debug/support/manifest")
    Call<SupportBundleNodeManifest> getNodeManifest();

    @POST("system/debug/support/bundle/build")
    Call<Response> buildSupportBundle();

    @GET("system/debug/support/logfile/{id}")
    @Streaming
    Call<ResponseBody> getLogFile(@Path("id") String id);
}
