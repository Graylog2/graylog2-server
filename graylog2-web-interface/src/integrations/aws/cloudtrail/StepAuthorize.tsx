import React, { useEffect, useContext, useState } from 'react';
import FormDataContext from 'integrations/contexts/FormDataContext';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import { ApiRoutes } from './common/Routes';
import formValidation from 'integrations/aws/utils/formValidation';
import type { ErrorMessageType, HandleFieldUpdateType, HandleSubmitType } from './types';

import { renderOptions } from '../common/Options';
import FormWrap from '../common/FormWrap';
import ValidatedInput from '../common/ValidatedInput';
import AWSAuthenticationTypes from './AWSAuthenticationTypes';

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

  const { awsSecret } = formData;
  const [loading, setLoading] = useState(false);

  const handleSubmit = () => {
    setLoading(true);

    fetch('POST', qualifyUrl(ApiRoutes.INTEGRATIONS.AWSCloudTrail.CHECK_CREDENTIALS), {
      aws_access_key: formData?.awsAccessKey?.value || '',
      aws_secret_key: formData?.awsSecret?.value || '',
      cloudtrail_queue_name: formData?.awsCloudTrailSqsQueueName?.value || '',
      aws_region: formData?.awsCloudTrailRegion?.value || '',
      assume_role_arn: formData?.assumeRoleArn?.value || '',
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
    ['awsCloudTrailName', 'awsAccessKey', 'awsSecret', 'awsCloudTrailRegion'],
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
        defaultValue={awsSecret?.value}
        required
      />

      <AWSAuthenticationTypes onChange={onChange} />

      <ValidatedInput
        type="select"
        id="awsCloudTrailRegion"
        onChange={onChange}
        fieldData={formData.awsCloudTrailRegion}
        help="CloudTrail region where the sqs queue is created."
        required
        label="CloudTrail Region">
        {renderOptions(regions, 'Choose your account region', false)}
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
      <ValidatedInput
        type="text"
        id="assumeRoleArn"
        fieldData={formData.assumeRoleArn}
        label="AWS Assume Role (ARN)"
        onChange={onChange}
        help="Amazon Resource Name with required cross account permission"
        placeholder="arn:aws:sts::123456789012:assumed-role/some-role"
        maxLength={2048}
      />
    </FormWrap>
  );
};

export default StepAuthorize;
