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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;

public class FreeEnterpriseService {
    private static final Logger LOG = LoggerFactory.getLogger(FreeEnterpriseService.class);

    private final FreeLicenseAPIClient apiClient;

    @Inject
    public FreeEnterpriseService(OkHttpClient httpClient,
                                 ObjectMapper objectMapper,
                                 @Named(FreeEnterpriseConfiguration.SERVICE_URL) URI serviceUrl) {
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(serviceUrl.toString())
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .client(httpClient)
                .build();
        this.apiClient = retrofit.create(FreeLicenseAPIClient.class);
    }

    public void requestFreeLicense(FreeLicenseRequest request) {
        final FreeLicenseAPIRequest apiRequest = FreeLicenseAPIRequest.builder()
                .clusterId(request.clusterId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .company(request.company())
                .build();
        try {
            final Response<FreeLicenseAPIResponse> response = apiClient.requestFreeLicense(apiRequest).execute();

            if (response.isSuccessful() && response.body() != null) {
                // TODO: Change to debug
                LOG.info("Received free Graylog Enterprise license: {}", response.body());
                // TODO: - Save license to cluster config so the license system can pick it up
                //       - Emit an event to the event bus for the license system to pick up and install the license
            } else {
                if (response.errorBody() != null) {
                    LOG.error("Couldn't request free Graylog Enterprise license: {} (code={})", response.errorBody().string(), response.code());
                } else {
                    LOG.error("Couldn't request free Graylog Enterprise license: {} (code={}, message=\"{}\")", response.message(), response.code(), response.message());
                }
                throw new FreeLicenseRequestException("Couldn't request free Graylog Enterprise license", request);
            }
        } catch (IOException e) {
            LOG.error("Couldn't request free Graylog Enterprise license", e);
            throw new FreeLicenseRequestException("Couldn't request free Graylog Enterprise license", request, e);
        }
    }
}
