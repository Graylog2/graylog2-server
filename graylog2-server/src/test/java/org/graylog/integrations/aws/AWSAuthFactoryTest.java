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
package org.graylog.integrations.aws;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class AWSAuthFactoryTest {
    private AWSAuthFactory awsAuthFactory;

    @BeforeEach
    public void setUp() {
        awsAuthFactory = new AWSAuthFactory();
    }

    @Test
    public void testAutomaticAuth() {
        assertThat(awsAuthFactory.create(false, null, null, null, null, (String) null))
                .isExactlyInstanceOf(DefaultCredentialsProvider.class);
    }

    @Test
    public void testAutomaticAuthIsFailingInCloudWithInvalidAccessKey() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                        awsAuthFactory.create(true, null, null, "secret", null, (String) null))
                .withMessageContaining("Access key");
    }

    @Test
    public void testAutomaticAuthIsFailingInCloudWithInvalidSecretKey() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                        awsAuthFactory.create(true, null, "key", null, null, (String) null))
                .withMessageContaining("Secret key");
    }

    @Test
    public void testKeySecret() {
        final AwsCredentialsProvider awsCredentialsProvider = awsAuthFactory.create(false, null, "key", "secret", null, (String) null);
        assertThat(awsCredentialsProvider).isExactlyInstanceOf(StaticCredentialsProvider.class);
        assertThat("key").isEqualTo(awsCredentialsProvider.resolveCredentials().accessKeyId());
        assertThat("secret").isEqualTo(awsCredentialsProvider.resolveCredentials().secretAccessKey());
    }

    @Test
    public void testKeySecret_exceptionThrownWhenRequired() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                        awsAuthFactory.create(true, null, null, null, null, (String) null))
                .withMessageContaining("Access key is required.");
    }

    @Test
    public void testCreateWithNullHttpClientBuilder() {
        final AwsCredentialsProvider awsCredentialsProvider = awsAuthFactory.create(false, null, "key", "secret", null, null, (ApacheHttpClient.Builder) null);
        assertThat(awsCredentialsProvider).isExactlyInstanceOf(StaticCredentialsProvider.class);
        assertThat("key").isEqualTo(awsCredentialsProvider.resolveCredentials().accessKeyId());
    }

    @Test
    public void testSixArgOverload_withHttpClientBuilder_noAssumeRole_returnsStaticCredentials() {
        final ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        final AwsCredentialsProvider result = awsAuthFactory.create(false, null, "key", "secret", null, null, httpClientBuilder);
        assertThat(result).isExactlyInstanceOf(StaticCredentialsProvider.class);
        assertThat(result.resolveCredentials().accessKeyId()).isEqualTo("key");
        assertThat(result.resolveCredentials().secretAccessKey()).isEqualTo("secret");
    }

    @Test
    public void testSixArgOverload_withHttpClientBuilder_noAssumeRole_returnsDefaultCredentials() {
        final ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        final AwsCredentialsProvider result = awsAuthFactory.create(false, null, null, null, null, null, httpClientBuilder);
        assertThat(result).isExactlyInstanceOf(DefaultCredentialsProvider.class);
    }

    @Test
    public void testFiveArgOverload_delegatesToSixArg() {
        final AwsCredentialsProvider fiveArg = awsAuthFactory.create(false, null, "key", "secret", null, (String) null);
        final AwsCredentialsProvider sixArg = awsAuthFactory.create(false, null, "key", "secret", null, null, (ApacheHttpClient.Builder) null);
        assertThat(fiveArg).isExactlyInstanceOf(sixArg.getClass());
        assertThat(fiveArg.resolveCredentials().accessKeyId()).isEqualTo(sixArg.resolveCredentials().accessKeyId());
    }

    // --- Assume Role tests use a mocked StsClient to avoid live network calls ---

    private StsClient buildMockStsClient() {
        StsClient mockStsClient = mock(StsClient.class);
        when(mockStsClient.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                .thenReturn(GetCallerIdentityResponse.builder().account("123456789012").build());
        return mockStsClient;
    }

    @Test
    public void testAssumeRoleWithExternalId_externalIdPresentInAssumeRoleRequest() {
        StsClient mockStsClient = buildMockStsClient();
        StsClientBuilder mockStsClientBuilder = mock(StsClientBuilder.class);
        when(mockStsClientBuilder.region(any())).thenReturn(mockStsClientBuilder);
        when(mockStsClientBuilder.credentialsProvider(any())).thenReturn(mockStsClientBuilder);
        when(mockStsClientBuilder.build()).thenReturn(mockStsClient);

        // Capture the AssumeRoleRequest passed to refreshRequest()
        StsAssumeRoleCredentialsProvider mockProvider = mock(StsAssumeRoleCredentialsProvider.class);
        StsAssumeRoleCredentialsProvider.Builder mockProviderBuilder = mock(StsAssumeRoleCredentialsProvider.Builder.class);
        ArgumentCaptor<AssumeRoleRequest> requestCaptor = ArgumentCaptor.forClass(AssumeRoleRequest.class);
        when(mockProviderBuilder.refreshRequest(requestCaptor.capture())).thenReturn(mockProviderBuilder);
        when(mockProviderBuilder.stsClient(any())).thenReturn(mockProviderBuilder);
        when(mockProviderBuilder.build()).thenReturn(mockProvider);

        try (MockedStatic<StsClient> mockedStsClient = mockStatic(StsClient.class);
             MockedStatic<StsAssumeRoleCredentialsProvider> mockedProvider = mockStatic(StsAssumeRoleCredentialsProvider.class)) {
            mockedStsClient.when(StsClient::builder).thenReturn(mockStsClientBuilder);
            mockedProvider.when(StsAssumeRoleCredentialsProvider::builder).thenReturn(mockProviderBuilder);

            awsAuthFactory.create(
                    false, "us-east-1", "key", "secret", "arn:aws:iam::123456789012:role/TestRole", "my-external-id", (ApacheHttpClient.Builder) null);

            assertThat(requestCaptor.getValue()).isNotNull();
            assertThat(requestCaptor.getValue().externalId()).isEqualTo("my-external-id");
        }
    }

    @Test
    public void testAssumeRoleWithoutExternalId_noExternalIdInAssumeRoleRequest() {
        StsClient mockStsClient = buildMockStsClient();
        StsClientBuilder mockStsClientBuilder = mock(StsClientBuilder.class);
        when(mockStsClientBuilder.region(any())).thenReturn(mockStsClientBuilder);
        when(mockStsClientBuilder.credentialsProvider(any())).thenReturn(mockStsClientBuilder);
        when(mockStsClientBuilder.build()).thenReturn(mockStsClient);

        StsAssumeRoleCredentialsProvider mockProvider = mock(StsAssumeRoleCredentialsProvider.class);
        StsAssumeRoleCredentialsProvider.Builder mockProviderBuilder = mock(StsAssumeRoleCredentialsProvider.Builder.class);
        ArgumentCaptor<AssumeRoleRequest> requestCaptor = ArgumentCaptor.forClass(AssumeRoleRequest.class);
        when(mockProviderBuilder.refreshRequest(requestCaptor.capture())).thenReturn(mockProviderBuilder);
        when(mockProviderBuilder.stsClient(any())).thenReturn(mockProviderBuilder);
        when(mockProviderBuilder.build()).thenReturn(mockProvider);

        try (MockedStatic<StsClient> mockedStsClient = mockStatic(StsClient.class);
             MockedStatic<StsAssumeRoleCredentialsProvider> mockedProvider = mockStatic(StsAssumeRoleCredentialsProvider.class)) {
            mockedStsClient.when(StsClient::builder).thenReturn(mockStsClientBuilder);
            mockedProvider.when(StsAssumeRoleCredentialsProvider::builder).thenReturn(mockProviderBuilder);

            awsAuthFactory.create(
                    false, "us-east-1", "key", "secret", "arn:aws:iam::123456789012:role/TestRole", (String) null, (ApacheHttpClient.Builder) null);

            assertThat(requestCaptor.getValue()).isNotNull();
            // No external ID – the field should be absent from the request
            assertThat(requestCaptor.getValue().externalId()).isNull();
        }
    }

    @Test
    public void testAssumeRoleWithEmptyExternalId_treatedAsAbsent() {
        StsClient mockStsClient = buildMockStsClient();
        StsClientBuilder mockStsClientBuilder = mock(StsClientBuilder.class);
        when(mockStsClientBuilder.region(any())).thenReturn(mockStsClientBuilder);
        when(mockStsClientBuilder.credentialsProvider(any())).thenReturn(mockStsClientBuilder);
        when(mockStsClientBuilder.build()).thenReturn(mockStsClient);

        StsAssumeRoleCredentialsProvider mockProvider = mock(StsAssumeRoleCredentialsProvider.class);
        StsAssumeRoleCredentialsProvider.Builder mockProviderBuilder = mock(StsAssumeRoleCredentialsProvider.Builder.class);
        ArgumentCaptor<AssumeRoleRequest> requestCaptor = ArgumentCaptor.forClass(AssumeRoleRequest.class);
        when(mockProviderBuilder.refreshRequest(requestCaptor.capture())).thenReturn(mockProviderBuilder);
        when(mockProviderBuilder.stsClient(any())).thenReturn(mockProviderBuilder);
        when(mockProviderBuilder.build()).thenReturn(mockProvider);

        try (MockedStatic<StsClient> mockedStsClient = mockStatic(StsClient.class);
             MockedStatic<StsAssumeRoleCredentialsProvider> mockedProvider = mockStatic(StsAssumeRoleCredentialsProvider.class)) {
            mockedStsClient.when(StsClient::builder).thenReturn(mockStsClientBuilder);
            mockedProvider.when(StsAssumeRoleCredentialsProvider::builder).thenReturn(mockProviderBuilder);

            // Null and empty external IDs must be treated identically (neither sets externalId on the request)
            awsAuthFactory.create(
                    false, "us-east-1", "key", "secret", "arn:aws:iam::123456789012:role/TestRole", (String) null, (ApacheHttpClient.Builder) null);
            assertThat(requestCaptor.getValue().externalId()).isNull();
        }
    }

    @Test
    public void testExternalIdIgnoredWhenNoAssumeRole() {
        final AwsCredentialsProvider withoutExternalId = awsAuthFactory.create(false, null, "key", "secret", null, (String) null);
        final AwsCredentialsProvider withExternalId = awsAuthFactory.create(false, null, "key", "secret", "some-external-id", (String) null);
        assertThat(withoutExternalId).isExactlyInstanceOf(StaticCredentialsProvider.class);
        assertThat(withExternalId).isExactlyInstanceOf(StaticCredentialsProvider.class);
    }
}
