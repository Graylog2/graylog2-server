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
import React, { useContext } from 'react';
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import FormDataContext from 'integrations/contexts/FormDataContext';
import useFetch from 'integrations/hooks/useFetch';
import { StatusIcon } from 'components/common';
import Store from 'logic/local-storage/Store';

import { ApiRoutes } from './common/Routes';
import { toAWSCloudTrailInputCreateRequest } from './common/formDataAdapter';
import type { HandleSubmitType } from './types';

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
  const { t } = useTranslation('integrations');
  const { formData } = useContext(FormDataContext);

  const throttleEnabled = !!formData.awsCloudTrailThrottleEnabled?.value;

  const [saveInput, setSaveInput] = useFetch(
    null,
    (result) => {
      if (result?.id) {
        Store.sessionSet('setup_wizard_input_id', result.id);
      }

      onSubmit();
    },
    'POST',
    toAWSCloudTrailInputCreateRequest(formData),
  );

  // Derive error state directly from saveInput.error
  const formError = saveInput.error
    ? {
        full_message: saveInput.error,
        nice_message: <span>{t('aws_cloudtrail_step_review.save_error')}</span>,
      }
    : null;

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
      buttonContent={t('aws_cloudtrail_step_review.save_button')}
      loading={saveInput.loading}
      error={formError}
      description={t('aws_cloudtrail_step_review.description')}>
      <Container>
        <Subheader>{t('aws_cloudtrail_step_review.input_configuration')}</Subheader>
        <ReviewItems>
          <li>
            <strong>{t('aws_cloudtrail_step_review.label_name')}</strong>
            <span>{formData.awsCloudTrailName.value}</span>
          </li>
          {formData.awsAccessKey?.value && (
            <li>
              <strong>{t('aws_cloudtrail_step_review.label_access_key')}</strong>
              <span>{formData.awsAccessKey?.value}</span>
            </li>
          )}
          <li>
            <strong>{t('aws_cloudtrail_step_review.label_sqs_region')}</strong>
            <span>{formData.awsCloudTrailSqsRegion.value}</span>
          </li>
          <li>
            <strong>{t('aws_cloudtrail_step_review.label_s3_region')}</strong>
            <span>{formData.awsCloudTrailS3Region.value}</span>
          </li>
          {formData.awsAssumeRoleARN?.value && (
            <li>
              <strong>{t('aws_cloudtrail_step_review.label_assume_role_arn')}</strong>
              <span>{formData.awsAssumeRoleARN?.value}</span>
            </li>
          )}
          {formData.awsExternalId?.value && (
            <li>
              <strong>{t('aws_cloudtrail_step_review.label_external_id')}</strong>
              <span>{formData.awsExternalId?.value}</span>
            </li>
          )}
          <li>
            <strong>{t('aws_cloudtrail_step_review.label_sqs_queue_name')}</strong>
            <span>{formData.awsCloudTrailSqsQueueName?.value}</span>
          </li>
          <li>
            <strong>{t('aws_cloudtrail_step_review.label_sqs_batch_size')}</strong>
            <span>{formData.sqsMessageBatchSize?.value}</span>
          </li>
          <li>
            <strong>{t('aws_cloudtrail_step_review.label_polling_interval')}</strong>
            <span>{formData.pollingInterval.value}</span>
          </li>
          <li>
            <strong>{t('aws_cloudtrail_step_review.label_enable_throttling')}</strong>
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
