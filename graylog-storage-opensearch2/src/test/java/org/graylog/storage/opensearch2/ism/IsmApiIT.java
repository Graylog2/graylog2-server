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
package org.graylog.storage.opensearch2.ism;

import org.graylog.storage.opensearch2.ism.policy.IsmPolicy;
import org.graylog.storage.opensearch2.ism.policy.IsmPolicyTest;
import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class IsmApiIT {

    @Rule
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    IsmApi ismApi = new IsmApi(openSearchInstance.openSearchClient(), new ObjectMapperProvider().get());

    @Test
    public void testGetUnknownPolicyReturnsEmpty() {
        final Optional<IsmPolicy> policy = ismApi.getPolicy("unknown");
        assertThat(policy).isEmpty();
    }

    @Test
    public void testCreatePolicyAndGet() {
        IsmPolicy policy = IsmPolicyTest.createSimpleTestPolicy();
        final String testPolicy = policy.id();
        ismApi.createPolicy(testPolicy, policy);
        Optional<IsmPolicy> storedPolicy = ismApi.getPolicy(testPolicy);
        assertThat(storedPolicy).isPresent();
        assertThat(storedPolicy.get().id()).isEqualTo(testPolicy);
        ismApi.deletePolicy(testPolicy);
    }

}
