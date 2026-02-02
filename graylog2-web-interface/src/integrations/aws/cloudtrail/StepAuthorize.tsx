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

import FormDataContext from 'integrations/contexts/FormDataContext';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import formValidation from 'integrations/aws/utils/formValidation';

import { ApiRoutes } from './common/Routes';
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
  const [regions, setRegions] = useState([]);

  useEffect(() => {
    fetch('GET', qualifyUrl(ApiRoutes.INTEGRATIONS.AWSCloudTrail.GET_AWS_REGIONS)).then((response) => {
      const region = Object.keys(response).map((key) => ({ label: response[key], value: key }));
      setRegions(region);
    });
  }, []);

  const { formData } = useContext(FormDataContext);
  const [formError, setFormError] = useState<ErrorMessageType>(null);

  const { awsSecretKey } = formData;
  const [loading, setLoading] = useState(false);

  const handleSubmit = () => {
    setLoading(true);

    fetch('POST', qualifyUrl(ApiRoutes.INTEGRATIONS.AWSCloudTrail.CHECK_CREDENTIALS), {
      aws_access_key: formData?.awsAccessKey?.value || '',
      aws_secret_key: formData?.awsSecretKey?.value || '',
      aws_sqs_queue_name: formData?.awsCloudTrailSqsQueueName?.value || '',
      aws_sqs_region: formData?.awsCloudTrailSqsRegion?.value || '',
      aws_s3_region: formData?.awsCloudTrailS3Region?.value || '',
      assume_role_arn: formData?.awsAssumeRoleARN?.value || '',
    })
      .then((result: any) => {
        if (result.result === 'valid') {
          setFormError(null);
          onSubmit();
        } else {
          setFormError({
            full_message: result.result,
            nice_message: 'Unable to connect to AWS CloudTrail using provided configuration.',
          });
        }

        setLoading(false);
      })
      .catch((err) => {
        setLoading(false);

        setFormError({
          full_message: err.message,
          nice_message: 'Unable to connect to AWS CloudTrail using provided configuration.',
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
      buttonContent="Verify Connection &amp; Proceed"
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
        placeholder="Input Name"
        label="Input Name"
        autoComplete="off"
        help="Select a name of your new input that describes it."
        defaultValue={awsSecretKey?.value}
        required
      />

      <AWSAuthenticationTypes onChange={onChange} requireCredentials={false} />

      <ValidatedInput
        type="select"
        id="awsCloudTrailSqsRegion"
        onChange={onChange}
        fieldData={formData.awsCloudTrailSqsRegion}
        help="AWS region where the SQS queue is located."
        required
        label="AWS SQS Region">
        {renderOptions(regions, 'Choose SQS region', false)}
      </ValidatedInput>

      <ValidatedInput
        type="select"
        id="awsCloudTrailS3Region"
        onChange={onChange}
        fieldData={formData.awsCloudTrailS3Region}
        help="AWS region where the S3 bucket is located."
        required
        label="AWS S3 Region">
        {renderOptions(regions, 'Choose S3 region', false)}
      </ValidatedInput>

      <ValidatedInput
        className=""
        id="awsCloudTrailSqsQueueName"
        type="text"
        onChange={onChange}
        fieldData={formData.awsCloudTrailSqsQueueName}
        help="SQS queue name created by the CloudTrail Subscriber"
        label="SQS Queue Name"
        required
      />
    </FormWrap>
  );
};

export default StepAuthorize;
