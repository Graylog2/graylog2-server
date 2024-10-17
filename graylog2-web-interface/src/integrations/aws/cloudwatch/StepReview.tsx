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

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { Input } from 'components/bootstrap';
import { Icon } from 'components/common';
import { FormDataContext } from 'integrations/aws/context/FormData';
import { ApiContext } from 'integrations/aws/context/Api';
import useFetch from 'integrations/aws/common/hooks/useFetch';
import FormWrap from 'integrations/aws/common/FormWrap';
import { ApiRoutes } from 'integrations/aws/common/Routes';
import { DEFAULT_KINESIS_LOG_TYPE, KINESIS_LOG_TYPES } from 'integrations/aws/common/constants';

type DefaultProps = {
  value: string;
};

const Default = ({
  value,
}: DefaultProps) => (
  <>{value} <small>(default)</small></>
);

type StepReviewProps = {
  onSubmit: (...args: any[]) => void;
  onEditClick: (...args: any[]) => (...args: any[]) => void;
  externalInputSubmit?: boolean;
};

const StepReview = ({
  onSubmit,
  onEditClick,
  externalInputSubmit = false,
}: StepReviewProps) => {
  const [formError, setFormError] = useState(null);
  const { formData } = useContext(FormDataContext);
  const { logData } = useContext(ApiContext);
  const {
    awsAuthenticationType,
    awsCloudWatchAddFlowLogPrefix = { value: undefined },
    awsCloudWatchAssumeARN = { value: undefined },
    awsCloudWatchAwsKey = { value: undefined },
    awsCloudWatchAwsRegion,
    awsCloudWatchBatchSize,
    awsEndpointCloudWatch = { value: undefined },
    awsCloudWatchKinesisInputType = { value: DEFAULT_KINESIS_LOG_TYPE },
    awsCloudWatchKinesisStream,
    awsCloudWatchName,
    awsCloudWatchThrottleEnabled = { value: undefined },
    awsEndpointDynamoDB = { value: undefined },
    awsEndpointIAM = { value: undefined },
    awsEndpointKinesis = { value: undefined },
  } = formData;

  const throttleEnabled = !!awsCloudWatchThrottleEnabled.value;
  const addPrefix = !!awsCloudWatchAddFlowLogPrefix.value;

  const [fetchSubmitStatus, setSubmitFetch] = useFetch(
    null,
    () => {
      onSubmit();
    },
    'POST',
    {
      name: awsCloudWatchName.value,
      region: awsCloudWatchAwsRegion.value,
      aws_input_type: awsCloudWatchKinesisInputType.value,
      stream_name: awsCloudWatchKinesisStream.value,
      batch_size: Number(awsCloudWatchBatchSize.value || awsCloudWatchBatchSize.defaultValue),
      enable_throttling: throttleEnabled,
      add_flow_log_prefix: addPrefix,
    },
  );

  useEffect(() => {
    if (fetchSubmitStatus.error) {
      setFormError({
        full_message: fetchSubmitStatus.error,
        nice_message: <span>We were unable to save your Input, please try again in a few moments.</span>,
      });
    }
  }, [fetchSubmitStatus.error]);

  const handleSubmit = () => {
    if (externalInputSubmit) {
      onSubmit(formData);

      return;
    }

    setSubmitFetch(ApiRoutes.INTEGRATIONS.AWS.KINESIS.SAVE);
  };

  return (
    <FormWrap onSubmit={handleSubmit}
              buttonContent="Complete CloudWatch Setup"
              loading={fetchSubmitStatus.loading}
              error={formError}
              title="Final Review"
              description="Check out everything below to make sure it&apos;s correct, then click the button below to complete your CloudWatch setup!">

      <Container>
        <Subheader>Setting up CloudWatch <small><EditAnchor onClick={onEditClick('authorize')}>Edit</EditAnchor></small></Subheader>
        <ReviewItems>
          <li>
            <strong>Name</strong>
            <span>{awsCloudWatchName.value}</span>
          </li>

          <li>
            <strong>Authorization Type</strong>
            <span>{awsAuthenticationType.value}</span>
          </li>

          {awsCloudWatchAwsKey.value && (
            <li>
              <strong>AWS Key</strong>
              <span>{awsCloudWatchAwsKey.value}</span>
            </li>
          )}

          {awsCloudWatchAssumeARN.value && (
            <li>
              <strong>AWS Assumed ARN Role</strong>
              <span>{awsCloudWatchAssumeARN.value}</span>
            </li>
          )}

          {awsEndpointCloudWatch.value && (
            <li>
              <strong>CloudWatch VPC Endpoint</strong>
              <span>{awsEndpointCloudWatch.value}</span>
            </li>
          )}

          {awsEndpointDynamoDB.value && (
            <li>
              <strong>DynamoDB VPC Endpoint</strong>
              <span>{awsEndpointDynamoDB.value}</span>
            </li>
          )}

          {awsEndpointIAM.value && (
            <li>
              <strong>IAM VPC Endpoint</strong>
              <span>{awsEndpointIAM.value}</span>
            </li>
          )}

          {awsEndpointKinesis.value && (
            <li>
              <strong>Kinesis VPC Endpoint</strong>
              <span>{awsEndpointKinesis.value}</span>
            </li>
          )}

          <li>
            <strong>AWS Region</strong>
            <span>{awsCloudWatchAwsRegion.value}</span>
          </li>
        </ReviewItems>

        <Subheader>Setting up Kinesis <small><EditAnchor onClick={onEditClick('kinesis-setup')}>Edit</EditAnchor></small></Subheader>
        <ReviewItems>
          <li>
            <strong>Stream</strong>
            <span>{awsCloudWatchKinesisStream.value}</span>
          </li>
          <li>
            <strong>Global Input</strong>
            <span><Icon name="check" /></span>
          </li>
          <li>
            <strong>Record Batch Size</strong>
            <span>
              {
                awsCloudWatchBatchSize.value
                  ? awsCloudWatchBatchSize.value
                  : <Default value={awsCloudWatchBatchSize.defaultValue} />
              }
            </span>
          </li>
          <li>
            <strong>Enable Throttling</strong>
            <span><Icon name={throttleEnabled ? 'check_circle' : 'cancel'} /></span>
          </li>
          <li>
            <strong>Add Flow Log prefix to field names</strong>
            <span><Icon name={addPrefix ? 'check_circle' : 'cancel'} /></span>
          </li>
        </ReviewItems>

        <Subheader>Formatting</Subheader>
        <ReviewItems>
          <li>
            <strong>Log Type</strong>
            <span>{KINESIS_LOG_TYPES.find((type) => type.value === awsCloudWatchKinesisInputType.value).label}</span>
          </li>
        </ReviewItems>
        <p>If you need your logs parsed differently check out our <Link to={Routes.SYSTEM.PIPELINES.RULES}>Pipeline Rule</Link> for additional details and instructions.</p>

        <Input id="awsCloudWatchLog"
               type="textarea"
               label=""
               value={(logData && logData.message) || "We haven't received a response back from Amazon yet."}
               rows={10}
               disabled />
      </Container>
    </FormWrap>
  );
};

const Container = styled.div`
  border: 1px solid #a6afbd;
  margin: 25px 0;
  padding: 15px;
  border-radius: 4px;
`;

const Subheader = styled.h3`
  margin: 0 0 10px;
`;

const ReviewItems = styled.ul`
  list-style: none;
  margin: 0 0 25px 10px;
  padding: 0;

  li {
    padding: 5px;

    &:nth-of-type(odd) {
      background-color: rgb(220 225 229 / 40%);
    }
  }

  strong::after {
    content: ':';
    margin-right: 5px;
  }
`;

const EditAnchor = styled.a`
  font-size: 12px;
  margin-left: 5px;
  font-style: italic;
  cursor: pointer;

  &::before {
    content: '(';
  }

  &::after {
    content: ')';
  }
`;

export default StepReview;
