/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.freeenterprise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import okhttp3.OkHttpClient;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
    private final EventBus eventBus;
    private final ClusterConfigService clusterConfigService;
    private final MongoConnection mongoConnection;

    @Inject
    public FreeEnterpriseService(OkHttpClient httpClient,
                                 ObjectMapper objectMapper,
                                 EventBus eventBus,
                                 ClusterConfigService clusterConfigService,
                                 MongoConnection mongoConnection,
                                 @Named(FreeEnterpriseConfiguration.SERVICE_URL) URI serviceUrl) {
        this.eventBus = eventBus;
        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(serviceUrl.toString())
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .client(httpClient)
                .build();
        this.apiClient = retrofit.create(FreeLicenseAPIClient.class);
    }

    private boolean hasFreeLicenseStaged() {
        return clusterConfigService.get(StagedFreeEnterpriseLicense.class) != null;
    }

    private boolean hasLicenseInstalled() {
        return mongoConnection.getMongoDatabase().getCollection("licenses").countDocuments() > 0;
    }

    private String getClusterId() {
        final ClusterId clusterId = clusterConfigService.get(ClusterId.class);
        if (clusterId == null) {
            throw new IllegalStateException("Couldn't find cluster ID in cluster config");
        }
        return clusterId.clusterId();
    }

    public FreeLicenseInfo licenseInfo() {
        if (hasLicenseInstalled()) {
            return FreeLicenseInfo.installed();
        } else if (hasFreeLicenseStaged()) {
            return FreeLicenseInfo.staged();
        }
        return FreeLicenseInfo.absent();
    }

    public boolean canRequestFreeLicense() {
        return !hasFreeLicenseStaged() && !hasLicenseInstalled();
    }

    public void requestFreeLicense(FreeLicenseRequest request) {
        final String clusterId = getClusterId();
        final FreeLicenseAPIRequest apiRequest = FreeLicenseAPIRequest.builder()
                .clusterId(clusterId)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .company(request.company())
                .build();
        try {
            final Response<FreeLicenseAPIResponse> response = apiClient.requestFreeLicense(apiRequest).execute();

            if (response.isSuccessful() && response.body() != null) {
                LOG.debug("Received free Graylog Enterprise license: {}", response.body());
                final StagedFreeEnterpriseLicense dto = StagedFreeEnterpriseLicense.builder()
                        .clusterId(clusterId)
                        .license(response.body().licenseString())
                        .createdAt(DateTime.now(DateTimeZone.UTC))
                        .build();
                // Stage the received free license in the cluster config so the license system can pick it up on restart
                clusterConfigService.write(dto);
                // Also publish the license on the cluster event bus so the license system can already install it (if enterprise is already installed)
                eventBus.post(dto);
            } else {
                if (response.errorBody() != null) {
                    LOG.error("Couldn't request free Graylog Enterprise license: {} (code={})", response.errorBody().string(), response.code());
                } else {
                    LOG.error("Couldn't request free Graylog Enterprise license: {} (code={}, message=\"{}\")", response.message(), response.code(), response.message());
                }
                throw new FreeLicenseRequestException("Couldn't request free Graylog Enterprise license", request);
            }
        } catch (FreeLicenseRequestException e) {
            // no need to log this again
            throw e;
        } catch (IOException e) {
            LOG.error("Couldn't request free Graylog Enterprise license from remote service", e);
            throw new FreeLicenseRequestException("Couldn't request free Graylog Enterprise license from remote service", request, e);
        } catch (Exception e) {
            LOG.error("Couldn't request free Graylog Enterprise license", e);
            throw new FreeLicenseRequestException("Couldn't request free Graylog Enterprise license", request, e);
        }
    }
}
