package org.graylog2.rest.resources.system.inputs;

import org.graylog2.rest.models.system.inputs.responses.InputStatesList;
import retrofit.Call;
import retrofit.http.GET;

public interface RemoteInputStatesResource {
    @GET("/system/inputstates")
    Call<InputStatesList> list();
}
