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
  const storeFullMessage = !!formData.awsCloudTrailStoreFullMessage?.value;

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
      description="Check out everything below to make sure it's correct, then click the button below to complete your AWS Security Lake setup!">
      <Container>
        <Subheader>Input Configuration</Subheader>
        <ReviewItems>
          <li>
            <strong>Name</strong>
            <span>{formData.awsCloudTrailName.value}</span>
          </li>
          <li>
            <strong>AWS Access Key</strong>
            <span>{formData.awsAccessKey.value}</span>
          </li>
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
            <strong>Polling Interval</strong>
            <span>{formData.pollingInterval.value}</span>
          </li>
          <li>
            <strong>Enable Throttling</strong>
            <span>
              <StatusIcon active={throttleEnabled} />
            </span>
          </li>
          <li>
            <strong>Store Full Message</strong>
            <span>
              <StatusIcon active={storeFullMessage} />
            </span>
          </li>
        </ReviewItems>
      </Container>
    </FormWrap>
  );
};

export default StepReview;
