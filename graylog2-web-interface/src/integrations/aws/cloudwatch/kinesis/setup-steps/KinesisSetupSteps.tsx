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
import React, { useContext, useEffect, useState } from 'react';
import styled from 'styled-components';

import { ApiRoutes } from 'integrations/aws/common/Routes';
import useFetch from 'integrations/aws/common/hooks/useFetch';
import { FormDataContext } from 'integrations/aws/context/FormData';

import KinesisSetupStep from './KinesisSetupStep';

type KinesisSetupStepsProps = {
  onSuccess: (...args: any[]) => void;
  onError: (...args: any[]) => void;
};

const KinesisSetupSteps = ({
  onSuccess,
  onError,
}: KinesisSetupStepsProps) => {
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

const StepItems = styled.ul`
  list-style: none;
  margin: 0;
  padding: 0;
`;

export default KinesisSetupSteps;
