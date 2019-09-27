import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { ApiRoutes } from 'aws/common/Routes';
import useFetch from 'aws/common/hooks/useFetch';

import { FormDataContext } from 'aws/context/FormData';

import KinesisSetupStep from './KinesisSetupStep';

const KinesisSetupSteps = ({ onSuccess, onError }) => {
  const { formData } = useContext(FormDataContext);
  const [streamArn, setStreamArn] = useState(null);
  const [roleArn, setRoleArn] = useState(null);

  const [createSubsciptionProgress, setCreateSubsciptionUrl] = useFetch(
    null,
    () => {
      onSuccess();
    },
    'POST',
    {
      region: formData.awsCloudWatchAwsRegion.value,
      log_group_name: formData.awsCloudWatchAwsGroupName.value,
      filter_name: 'filter-name', // TODO: Use unique filter name
      filter_pattern: '',
      destination_stream_arn: streamArn,
      role_arn: roleArn,
    },
  );

  const [createPolicyProgress, setCreatePolicyUrl] = useFetch(
    null,
    (response) => {
      setRoleArn(response.role_arn);
      setCreateSubsciptionUrl(ApiRoutes.INTEGRATIONS.AWS.KINESIS_AUTO_SETUP.CREATE_SUBSCRIPTION);
    },
    'POST',
    {
      region: formData.awsCloudWatchAwsRegion.value,
      stream_name: formData.awsCloudWatchKinesisStream.value,
      stream_arn: streamArn,
    },
  );

  const [createStreamProgress, setCreateStreamUrl] = useFetch(
    null,
    (response) => {
      setStreamArn(response.stream_arn);
      setCreatePolicyUrl(ApiRoutes.INTEGRATIONS.AWS.KINESIS_AUTO_SETUP.CREATE_SUBSCRIPTION_POLICY);
    },
    'POST',
    {
      region: formData.awsCloudWatchAwsRegion.value,
      stream_name: formData.awsCloudWatchKinesisStream.value,
    },
  );

  useEffect(() => {
    setCreateStreamUrl(ApiRoutes.INTEGRATIONS.AWS.KINESIS_AUTO_SETUP.CREATE_STREAM);
  }, []);

  useEffect(() => {
    if (createStreamProgress.error || createPolicyProgress.error || createSubsciptionProgress.error) {
      onError();
    }
  }, [createStreamProgress.error, createPolicyProgress.error, createSubsciptionProgress.error]);

  return (
    <StepItems>
      <KinesisSetupStep label="Kinesis Stream" progress={createStreamProgress} />
      <KinesisSetupStep label="Subscription Policy" progress={createPolicyProgress} />
      <KinesisSetupStep label="Subscription" progress={createSubsciptionProgress} />
    </StepItems>
  );
};

KinesisSetupSteps.propTypes = {
  onSuccess: PropTypes.func.isRequired,
  onError: PropTypes.func.isRequired,
};

const StepItems = styled.ul`
  list-style: none;
  margin: 0;
  padding: 0;
`;

export default KinesisSetupSteps;
