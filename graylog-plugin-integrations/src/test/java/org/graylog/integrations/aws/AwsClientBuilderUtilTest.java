package org.graylog.integrations.aws;

import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;

import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AwsClientBuilderUtilTest {

    // Mock Objects
    @Mock private IamClientBuilder mockIamClientBuilder;
    @Mock private AWSRequest mockAwsRequest;

    // Test Objects
    IamClient iamClient;

    // Test Cases
    @Test
    public void buildClient_returnsNonGovIamClient_whenNonGovRegionNullEndpoint() {
        givenNonGovAwsRegion();
        givenGoodCredentialsProvider();
        givenNullEndpoint();
        givenBuilderSucceeds();

        whenBuildClientIsCalledForIam();

        thenIamClientIsReturned();
        thenIamClientConstructedWitoutEndpoint();
        thenIamClientConstructedWithNonGovGlobalRegion();
    }

    @Test
    public void buildClient_returnsGovIamClient_whenGovRegionNullEndpoint() {
        givenGovAwsRegion();
        givenGoodCredentialsProvider();
        givenNullEndpoint();
        givenBuilderSucceeds();

        whenBuildClientIsCalledForIam();

        thenIamClientIsReturned();
        thenIamClientConstructedWitoutEndpoint();
        thenIamClientConstructedWithGovGlobalRegion();
    }

    @Test
    public void buildClient_returnsNonGovIamClient_whenNonGovRegionEmptyEndpoint() {
        givenNonGovAwsRegion();
        givenGoodCredentialsProvider();
        givenNullEndpoint();
        givenBuilderSucceeds();

        whenBuildClientIsCalledForIam();

        thenIamClientIsReturned();
        thenIamClientConstructedWitoutEndpoint();
        thenIamClientConstructedWithNonGovGlobalRegion();
    }

    @Test
    public void buildClient_returnsGovIamClient_whenGovRegionProvidedEndpoint() {
        givenGovAwsRegion();
        givenGoodCredentialsProvider();
        givenGoodIamEndpoint();
        givenBuilderSucceeds();

        whenBuildClientIsCalledForIam();

        thenIamClientIsReturned();
        thenIamClientConstructedWithGovGlobalRegion();
    }

    // GIVENs
    private void givenNonGovAwsRegion() {
        given(mockAwsRequest.region()).willReturn("us-east-1");
    }

    private void givenGovAwsRegion() {
        given(mockAwsRequest.region()).willReturn("us-gov-west-1");
    }

    private void givenGoodCredentialsProvider() {
        given(mockAwsRequest.awsAccessKeyId()).willReturn("AKTESTTESTTEST");
        given(mockAwsRequest.awsSecretAccessKey()).willReturn("SECRETSECRETSECRET");
    }

    private void givenNullEndpoint() {
        given(mockAwsRequest.iamEndpoint()).willReturn(null);
    }

    private void givenEmptyEndpoint() {
        given(mockAwsRequest.iamEndpoint()).willReturn("");
    }

    private void givenGoodIamEndpoint() {
        given(mockAwsRequest.iamEndpoint()).willReturn("https://iam.amazonaws.com");
    }

    private void givenBuilderSucceeds() {
        given(mockIamClientBuilder.build()).willReturn(mock(IamClient.class));
    }

    // WHENs
    private void whenBuildClientIsCalledForIam() {
        iamClient = AWSClientBuilderUtil.buildClient(mockIamClientBuilder, mockAwsRequest);
    }

    // THENs
    private void thenIamClientIsReturned() {
        assertThat(iamClient, notNullValue());
    }

    private void thenIamClientConstructedWithNonGovGlobalRegion() {
        ArgumentCaptor<Region> regionCaptor = ArgumentCaptor.forClass(Region.class);
        verify(mockIamClientBuilder, times(1)).region(regionCaptor.capture());

        assertThat(regionCaptor.getValue(), notNullValue());
        Region region = regionCaptor.getValue();
        assertThat(region, is(Region.AWS_GLOBAL));
    }

    private void thenIamClientConstructedWithGovGlobalRegion() {
        ArgumentCaptor<Region> regionCaptor = ArgumentCaptor.forClass(Region.class);
        verify(mockIamClientBuilder, times(1)).region(regionCaptor.capture());

        assertThat(regionCaptor.getValue(), notNullValue());
        Region region = regionCaptor.getValue();
        assertThat(region, is(Region.AWS_US_GOV_GLOBAL));
    }

    private void thenIamClientConstructedWitoutEndpoint() {
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(mockIamClientBuilder, times(0)).endpointOverride(uriCaptor.capture());
    }

    private void thenIamClientConstructedWithEndpoint() {
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(mockIamClientBuilder, times(1)).endpointOverride(uriCaptor.capture());

        assertThat(uriCaptor.getValue(), notNullValue());
    }
}
