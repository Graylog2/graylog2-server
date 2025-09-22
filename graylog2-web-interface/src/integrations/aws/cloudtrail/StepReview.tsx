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
import styled, { css } from 'styled-components';

import FormDataContext from 'integrations/contexts/FormDataContext';
import useFetch from 'integrations/hooks/useFetch';
import { StatusIcon } from 'components/common';

import { ApiRoutes } from './common/Routes';
import { toAWSCloudTrailInputCreateRequest } from './common/formDataAdapter';
import type { ErrorMessageType, HandleSubmitType } from './types';

import FormWrap from '../common/FormWrap';

const Container = styled.div(
  ({ theme }) => css`
    border: 1px solid ${theme.colors.variant.darkest.default};
    margin: 25px 0;
    padding: 15px;
    border-radius: 4px;
    width: max-content;
  `,
);

const Subheader = styled.h3`
  margin: 0 0 10px;
`;

const ReviewItems = styled.ul(
  ({ theme }) => css`
    list-style: none;
    margin: 0 0 25px 10px;
    padding: 0;

    li {
      line-height: 2;
      padding: 0 5px;

      &:nth-of-type(odd) {
        background-color: ${theme.colors.table.row.backgroundStriped};
      }
    }

    strong::after {
      content: ':';
      margin-right: 5px;
    }
  `,
);

type Props = {
  onSubmit: HandleSubmitType;
  externalInputSubmit?: boolean;
};

const StepReview = ({ onSubmit, externalInputSubmit = false }: Props) => {
  const [formError, setFormError] = useState<ErrorMessageType>(null);
  const { formData } = useContext(FormDataContext);

  const throttleEnabled = !!formData.awsCloudTrailThrottleEnabled?.value;

  const [saveInput, setSaveInput] = useFetch(
    null,
    () => onSubmit(),
    'POST',
    toAWSCloudTrailInputCreateRequest(formData)
  );

  useEffect(() => {
    setSaveInput(null);

    if (saveInput.error) {
      setFormError({
        full_message: saveInput.error,
        nice_message: <span>We were unable to save your Input, please try again in a few moments.</span>,
      });
    }
  }, [saveInput.error, setSaveInput]);

  const handleSubmit = () => {
    if (externalInputSubmit) {
      onSubmit(formData);

      return;
    }

    setSaveInput(ApiRoutes.INTEGRATIONS.AWSCloudTrail.SAVE_INPUT);
  };

  return (
    <FormWrap
      onSubmit={handleSubmit}
      buttonContent="Save and Start Input"
      loading={saveInput.loading}
      error={formError}
      description="Check out everything below to make sure it's correct, then click the button below to complete your AWS CloudTrail setup!">
      <Container>
        <Subheader>Input Configuration</Subheader>
        <ReviewItems>
          <li>
            <strong>Name</strong>
            <span>{formData.awsCloudTrailName.value}</span>
          </li>
          {formData.awsAccessKey?.value && (
            <li>
              <strong>AWS Access Key: </strong>
              <span>{formData.awsAccessKey?.value}</span>
            </li>
          )}
          <li>
            <strong>Subscriber Region</strong>
            <span>{formData.awsCloudTrailRegion.value}</span>
          </li>
          {formData.assumeRoleArn?.value && (
            <li>
              <strong>AWS Assume Role (ARN): </strong>
              <span>{formData.assumeRoleArn?.value}</span>
            </li>
          )}
          <li>
            <strong>AWS SQS Queue name: </strong>
            <span>{formData.awsCloudTrailSqsQueueName?.value}</span>
          </li>
          <li>
            <strong>Polling Interval</strong>
            <span>{formData.pollingInterval.value}</span>
          </li>
          <li>
            <strong>Enable Throttling</strong>
            <span>
              <StatusIcon active={throttleEnabled} />
            </span>
          </li>
        </ReviewItems>
      </Container>
    </FormWrap>
  );
};

export default StepReview;
