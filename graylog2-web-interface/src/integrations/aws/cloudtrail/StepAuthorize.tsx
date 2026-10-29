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
import React, { useEffect, useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { AWSCloudTrail } from '@graylog/server-api';

import FormDataContext from 'integrations/contexts/FormDataContext';
import formValidation from 'integrations/aws/utils/formValidation';
import { validateExternalIdRequiresArn } from 'integrations/aws/utils/awsValidation';

import type { ErrorMessageType, HandleFieldUpdateType, HandleSubmitType } from './types';

import { renderOptions } from '../common/Options';
import FormWrap from '../common/FormWrap';
import ValidatedInput from '../common/ValidatedInput';
import AWSAuthenticationTypes from '../authentication/AWSAuthenticationTypes';

type StepAuthorizeProps = {
  onSubmit: HandleSubmitType;
  onChange: HandleFieldUpdateType;
};

const StepAuthorize = ({ onSubmit, onChange }: StepAuthorizeProps) => {
  const { t } = useTranslation('integrations');
  const [regions, setRegions] = useState([]);

  useEffect(() => {
    AWSCloudTrail.getAWSRegions().then((response) => {
      const region = Object.keys(response).map((key) => ({ label: response[key], value: key }));
      setRegions(region);
    });
  }, []);

  const { formData } = useContext(FormDataContext);
  const [formError, setFormError] = useState<ErrorMessageType>(null);

  const { awsSecretKey } = formData;
  const [loading, setLoading] = useState(false);

  const handleSubmit = () => {
    // Validate that External ID is only used with Assume Role ARN
    const externalIdError = validateExternalIdRequiresArn(
      formData?.awsAssumeRoleARN?.value,
      formData?.awsExternalId?.value,
    );

    if (externalIdError) {
      setFormError({
        full_message: externalIdError,
        nice_message: externalIdError,
      });

      return;
    }

    setLoading(true);

    AWSCloudTrail.checkCredentials({
      aws_access_key: formData?.awsAccessKey?.value || '',
      aws_secret_key: formData?.awsSecretKey?.value || '',
      aws_sqs_queue_name: formData?.awsCloudTrailSqsQueueName?.value || '',
      aws_sqs_region: formData?.awsCloudTrailSqsRegion?.value || '',
      aws_s3_region: formData?.awsCloudTrailS3Region?.value || '',
      assume_role_arn: formData?.awsAssumeRoleARN?.value || '',
      aws_external_id: formData?.awsExternalId?.value || '',
    })
      .then((result) => {
        if (result.result === 'valid') {
          setFormError(null);
          onSubmit();
        } else {
          setFormError({
            full_message: result.result,
            nice_message: t('aws_cloudtrail_step_authorize.connection_error'),
          });
        }

        setLoading(false);
      })
      .catch((err) => {
        setLoading(false);

        setFormError({
          full_message: err.message,
          nice_message: t('aws_cloudtrail_step_authorize.connection_error'),
        });
      });
  };

  const isFormValid = formValidation.isFormValid(
    ['awsCloudTrailName', 'awsCloudTrailSqsRegion', 'awsCloudTrailS3Region', 'awsCloudTrailSqsQueueName'],
    formData,
  );

  return (
    <FormWrap
      onSubmit={handleSubmit}
      buttonContent={t('aws_cloudtrail_step_authorize.verify_button')}
      disabled={isFormValid}
      loading={loading}
      title=""
      error={formError}
      description="">
      <ValidatedInput
        className=""
        id="awsCloudTrailName"
        type="text"
        fieldData={formData.awsCloudTrailName}
        onChange={onChange}
        placeholder={t('aws_cloudtrail_step_authorize.input_name')}
        label={t('aws_cloudtrail_step_authorize.input_name')}
        autoComplete="off"
        help={t('aws_cloudtrail_step_authorize.input_name_help')}
        defaultValue={awsSecretKey?.value}
        required
      />

      <AWSAuthenticationTypes onChange={onChange} requireCredentials={false} />

      <ValidatedInput
        type="select"
        id="awsCloudTrailSqsRegion"
        onChange={onChange}
        fieldData={formData.awsCloudTrailSqsRegion}
        help={t('aws_cloudtrail_step_authorize.sqs_region_help')}
        required
        label={t('aws_cloudtrail_step_authorize.sqs_region_label')}>
        {renderOptions(regions, t('aws_cloudtrail_step_authorize.choose_sqs_region'), false)}
      </ValidatedInput>

      <ValidatedInput
        type="select"
        id="awsCloudTrailS3Region"
        onChange={onChange}
        fieldData={formData.awsCloudTrailS3Region}
        help={t('aws_cloudtrail_step_authorize.s3_region_help')}
        required
        label={t('aws_cloudtrail_step_authorize.s3_region_label')}>
        {renderOptions(regions, t('aws_cloudtrail_step_authorize.choose_s3_region'), false)}
      </ValidatedInput>

      <ValidatedInput
        className=""
        id="awsCloudTrailSqsQueueName"
        type="text"
        onChange={onChange}
        fieldData={formData.awsCloudTrailSqsQueueName}
        help={t('aws_cloudtrail_step_authorize.sqs_queue_name_help')}
        label={t('aws_cloudtrail_step_authorize.sqs_queue_name_label')}
        required
      />
    </FormWrap>
  );
};

export default StepAuthorize;
