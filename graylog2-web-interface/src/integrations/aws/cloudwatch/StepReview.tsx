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
import styled from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import { Link, Icon, StatusIcon } from 'components/common';
import Routes from 'routing/Routes';
import { Input } from 'components/bootstrap';
import FormDataContext from 'integrations/contexts/FormDataContext';
import { ApiContext } from 'integrations/aws/context/Api';
import useFetch from 'integrations/hooks/useFetch';
import FormWrap from 'integrations/aws/common/FormWrap';
import { ApiRoutes } from 'integrations/aws/common/Routes';
import { DEFAULT_KINESIS_LOG_TYPE, KINESIS_LOG_TYPES } from 'integrations/aws/common/constants';
import { toAWSRequest } from 'integrations/aws/common/formDataAdapter';
import Store from 'logic/local-storage/Store';

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

const ArnErrorMessage = styled.span`
  color: #856404;
  background-color: #fff3cd;
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
`;

type DefaultProps = {
  value: string;
};

const Default = ({ value }: DefaultProps) => {
  const { t } = useTranslation('integrations');

  return (
    <>
      {value} <small>{t('aws_cloudwatch_step_review.default_label')}</small>
    </>
  );
};

type StepReviewProps = {
  onSubmit: (...args: any[]) => void;
  onEditClick: (...args: any[]) => (...args: any[]) => void;
  externalInputSubmit?: boolean;
};

const StepReview = ({ onSubmit, onEditClick, externalInputSubmit = false }: StepReviewProps) => {
  const { t } = useTranslation('integrations');
  const { formData } = useContext(FormDataContext);
  const { logData } = useContext(ApiContext);
  const {
    awsAuthenticationType,
    awsCloudWatchAddFlowLogPrefix = { value: undefined },
    awsCloudWatchStoreFullMessage = { value: undefined },
    awsAssumeRoleARN = { value: undefined },
    awsExternalId = { value: undefined },
    awsAccessKey = { value: undefined },
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
    overrideSource = { value: undefined },
  } = formData;

  const throttleEnabled = !!awsCloudWatchThrottleEnabled.value;
  const addPrefix = !!awsCloudWatchAddFlowLogPrefix.value;
  const storeFullMessage = !!awsCloudWatchStoreFullMessage.value;
  const awsCloudwatchKinesisStreamArn = formData.awsCloudwatchKinesisStreamArn?.value ?? '';

  const [fetchSubmitStatus, setSubmitFetch] = useFetch(
    null,
    (result) => {
      if (result?.id) {
        Store.sessionSet('setup_wizard_input_id', result.id);
      }

      onSubmit();
    },
    'POST',
    toAWSRequest(formData, {
      name: awsCloudWatchName.value,
      region: awsCloudWatchAwsRegion.value,
      aws_input_type: awsCloudWatchKinesisInputType.value,
      stream_name: awsCloudWatchKinesisStream.value,
      batch_size: Number(awsCloudWatchBatchSize.value || awsCloudWatchBatchSize.defaultValue),
      enable_throttling: throttleEnabled,
      add_flow_log_prefix: addPrefix,
      store_full_message: storeFullMessage,
      kinesis_stream_arn: awsCloudwatchKinesisStreamArn,
      aws_external_id: awsExternalId.value ?? '',
      override_source: overrideSource?.value ?? '',
    }),
  );

  const formError = fetchSubmitStatus.error
    ? {
        full_message: fetchSubmitStatus.error,
        nice_message: <span>{t('aws_cloudwatch_step_review.save_error')}</span>,
      }
    : null;

  const handleSubmit = () => {
    if (externalInputSubmit) {
      onSubmit(formData);

      return;
    }

    setSubmitFetch(ApiRoutes.INTEGRATIONS.AWS.KINESIS.SAVE);
  };

  return (
    <FormWrap
      onSubmit={handleSubmit}
      buttonContent={t('aws_cloudwatch_step_review.complete_button')}
      loading={fetchSubmitStatus.loading}
      error={formError}
      title={t('aws_cloudwatch_step_review.title')}
      description={t('aws_cloudwatch_step_review.description')}>
      <Container>
        <Subheader>
          {t('aws_cloudwatch_step_review.setting_up_cloudwatch')}{' '}
          <small>
            <EditAnchor onClick={onEditClick('authorize')}>{t('aws_cloudwatch_step_review.edit')}</EditAnchor>
          </small>
        </Subheader>
        <ReviewItems>
          <li>
            <strong>{t('aws_cloudwatch_step_review.label_name')}</strong>
            <span>{awsCloudWatchName.value}</span>
          </li>

          <li>
            <strong>{t('aws_cloudwatch_step_review.label_authorization_type')}</strong>
            <span>{awsAuthenticationType.value}</span>
          </li>

          {awsAccessKey.value && (
            <li>
              <strong>{t('aws_cloudwatch_step_review.label_aws_key')}</strong>
              <span>{awsAccessKey.value}</span>
            </li>
          )}

          {awsAssumeRoleARN.value && (
            <li>
              <strong>{t('aws_cloudwatch_step_review.label_assumed_role_arn')}</strong>
              <span>{awsAssumeRoleARN.value}</span>
            </li>
          )}

          {awsExternalId.value && (
            <li>
              <strong>{t('aws_cloudwatch_step_review.label_external_id')}</strong>
              <span>{awsExternalId.value}</span>
            </li>
          )}

          {awsEndpointCloudWatch.value && (
            <li>
              <strong>{t('aws_cloudwatch_step_review.label_cloudwatch_endpoint')}</strong>
              <span>{awsEndpointCloudWatch.value}</span>
            </li>
          )}

          {awsEndpointDynamoDB.value && (
            <li>
              <strong>{t('aws_cloudwatch_step_review.label_dynamodb_endpoint')}</strong>
              <span>{awsEndpointDynamoDB.value}</span>
            </li>
          )}

          {awsEndpointIAM.value && (
            <li>
              <strong>{t('aws_cloudwatch_step_review.label_iam_endpoint')}</strong>
              <span>{awsEndpointIAM.value}</span>
            </li>
          )}

          {awsEndpointKinesis.value && (
            <li>
              <strong>{t('aws_cloudwatch_step_review.label_kinesis_endpoint')}</strong>
              <span>{awsEndpointKinesis.value}</span>
            </li>
          )}

          <li>
            <strong>{t('aws_cloudwatch_step_review.label_aws_region')}</strong>
            <span>{awsCloudWatchAwsRegion.value}</span>
          </li>
        </ReviewItems>

        <Subheader>
          {t('aws_cloudwatch_step_review.setting_up_kinesis')}{' '}
          <small>
            <EditAnchor onClick={onEditClick('kinesis-setup')}>{t('aws_cloudwatch_step_review.edit')}</EditAnchor>
          </small>
        </Subheader>
        <ReviewItems>
          <li>
            <strong>{t('aws_cloudwatch_step_review.label_kinesis_stream')}</strong>
            <span>{awsCloudWatchKinesisStream.value}</span>
          </li>
          <li>
            <strong>{t('aws_cloudwatch_step_review.label_kinesis_stream_arn')}</strong>
            <span>
              {!awsCloudwatchKinesisStreamArn ? (
                <ArnErrorMessage>{t('aws_cloudwatch_step_review.stream_arn_error')}</ArnErrorMessage>
              ) : (
                awsCloudwatchKinesisStreamArn
              )}
            </span>
          </li>

          <li>
            <strong>{t('aws_cloudwatch_step_review.label_global_input')}</strong>
            <span>
              <Icon name="check" />
            </span>
          </li>
          <li>
            <strong>{t('aws_cloudwatch_step_review.label_record_batch_size')}</strong>
            <span>
              {awsCloudWatchBatchSize.value ? (
                awsCloudWatchBatchSize.value
              ) : (
                <Default value={awsCloudWatchBatchSize.defaultValue} />
              )}
            </span>
          </li>
          <li>
            <strong>{t('aws_cloudwatch_step_review.label_enable_throttling')}</strong>
            <span>
              <StatusIcon active={throttleEnabled} />
            </span>
          </li>
          <li>
            <strong>{t('aws_cloudwatch_step_review.label_flow_log_prefix')}</strong>
            <span>
              <StatusIcon active={addPrefix} />
            </span>
          </li>
          <li>
            <strong>{t('aws_cloudwatch_step_review.label_store_full_message')}</strong>
            <span>
              <StatusIcon active={storeFullMessage} />
            </span>
          </li>
          {overrideSource.value && (
            <li>
              <strong>{t('aws_cloudwatch_step_review.label_override_source')}</strong>
              <span>{overrideSource.value}</span>
            </li>
          )}
        </ReviewItems>

        <Subheader>{t('aws_cloudwatch_step_review.formatting')}</Subheader>
        <ReviewItems>
          <li>
            <strong>{t('aws_cloudwatch_step_review.label_log_type')}</strong>
            <span>{KINESIS_LOG_TYPES.find((type) => type.value === awsCloudWatchKinesisInputType.value).label}</span>
          </li>
        </ReviewItems>
        <p>
          <Trans
            t={t}
            i18nKey="aws_cloudwatch_step_review.pipeline_hint"
            components={{ pipelineLink: <Link to={Routes.SYSTEM.PIPELINES.RULES} /> }}
          />
        </p>

        <Input
          id="awsCloudWatchLog"
          type="textarea"
          label=""
          value={(logData && logData.message) || t('aws_cloudwatch_step_review.no_response')}
          rows={10}
          disabled
        />
      </Container>
    </FormWrap>
  );
};

export default StepReview;
