package org.graylog2.shared.rest.resources.system;

import retrofit2.Call;
import retrofit2.http.POST;

public interface RemoteDeflectorResource {
    @POST("/system/deflector/cycle")
    Call<Void> cycle();
}
