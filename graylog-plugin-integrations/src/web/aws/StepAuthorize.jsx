import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { FormDataContext } from 'aws/context/FormData';
import { ApiContext } from 'aws/context/Api';
import { SidebarContext } from 'aws/context/Sidebar';

import ValidatedInput from 'aws/common/ValidatedInput';
import FormWrap from 'aws/common/FormWrap';
import { renderOptions } from 'aws/common/Options';
import { ApiRoutes } from 'aws/common/Routes';
import { AWS_AUTH_TYPES } from 'aws/common/constants';
import useFetch from 'aws/common/hooks/useFetch';

import formValidation from 'aws/utils/formValidation';
import AWSAuthenticationTypes from 'aws/authentication/AWSAuthenticationTypes';
import AWSCustomEndpoints from 'aws/authentication/AWSCustomEndpoints';

const StepAuthorize = ({ onChange, onSubmit, sidebarComponent }) => {
  const { formData } = useContext(FormDataContext);
  const { clearSidebar, setSidebar } = useContext(SidebarContext);
  const { availableRegions, setRegions, setStreams } = useContext(ApiContext);
  const [formError, setFormError] = useState(null);
  const [fetchRegionsStatus] = useFetch(ApiRoutes.INTEGRATIONS.AWS.REGIONS, setRegions, 'GET');
  const [fetchStreamsStatus, setStreamsFetch] = useFetch(
    null,
    (response) => {
      setStreams(response);
      onSubmit();
    },
    'POST',
    { region: formData.awsCloudWatchAwsRegion ? formData.awsCloudWatchAwsRegion.value : '' },
  );

  useEffect(() => {
    setStreamsFetch(null);
    if (fetchRegionsStatus.error) {
      setFormError({ full_message: fetchRegionsStatus.error });
    } else if (fetchStreamsStatus.error) {
      const badKey = /security token/g;
      const badSecret = /signing method/g;
      const noStreams = /No Kinesis streams/g;
      if (fetchStreamsStatus.error.match(badKey)) {
        setFormError({ full_message: fetchStreamsStatus.error, nice_message: 'Invalid AWS Key, check out your AWS account for the 20-character long, alphanumeric string that usually starts with the letters "AK"' });
      } else if (fetchStreamsStatus.error.match(badSecret)) {
        setFormError({ full_message: fetchStreamsStatus.error, nice_message: 'Invalid AWS Secret, it is usually a 40-character long, base-64 encoded string, but you only get to view it once when you create the Key' });
      } else if (fetchStreamsStatus.error.match(noStreams)) {
        // NOTE: If no streams are present we want to move to the KinesisSetup screen
        setStreams({ streams: [] });
        onSubmit();
      } else {
        setFormError({ full_message: fetchStreamsStatus.error });
      }
    }

    return () => {
      setFormError(null);
    };
  }, [fetchRegionsStatus.error, fetchStreamsStatus.error]);

  const handleSubmit = () => {
    setStreamsFetch(ApiRoutes.INTEGRATIONS.AWS.KINESIS.STREAMS);
  };

  useEffect(() => {
    if (sidebarComponent) {
      setSidebar(sidebarComponent);
    }

    return () => {
      clearSidebar();
    };
  }, []);

  const authType = formData.awsAuthenticationType && formData.awsAuthenticationType.value;
  const isFormValid = formValidation.isFormValid([
    'awsCloudWatchName',
    'awsCloudWatchAwsRegion',
    ...authType !== AWS_AUTH_TYPES.automatic ? ['awsCloudWatchAwsKey', 'awsCloudWatchAwsSecret'] : [],
  ], formData);

  return (
    <FormWrap onSubmit={handleSubmit}
              buttonContent="Authorize &amp; Choose Stream"
              loading={fetchRegionsStatus.loading || fetchStreamsStatus.loading}
              disabled={isFormValid}
              error={formError}
              title="Create Input &amp; Authorize AWS"
              description="This integration allows Graylog to read messages directly from a Kinesis stream. CloudWatch messages can optionally be forwarded to Kinesis via CloudWatch subscriptions and then read by Graylog.">

      <DisappearingInput id="name" type="text" />
      <DisappearingInput id="password" type="password" />

      <ValidatedInput id="awsCloudWatchName"
                      type="text"
                      fieldData={formData.awsCloudWatchName}
                      onChange={onChange}
                      placeholder="Graylog Input Name"
                      label="Graylog Input Name"
                      autoComplete="off"
                      required />

      <AWSAuthenticationTypes onChange={onChange} />

      <ValidatedInput id="awsCloudWatchAwsRegion"
                      type="select"
                      fieldData={formData.awsCloudWatchAwsRegion}
                      onChange={onChange}
                      label="AWS Region"
                      help="The AWS Region your service is running in."
                      disabled={fetchRegionsStatus.loading}
                      required>
        {renderOptions(availableRegions, 'Choose AWS Region', fetchRegionsStatus.loading)}
      </ValidatedInput>

      <AWSCustomEndpoints onChange={onChange} />
    </FormWrap>
  );
};

StepAuthorize.propTypes = {
  onSubmit: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,
  sidebarComponent: PropTypes.node,
};

StepAuthorize.defaultProps = {
  sidebarComponent: null,
};

const DisappearingInput = styled.input`
  position: fixed;
  top: -500vh;
  left: -500vw;
`;

export default StepAuthorize;
