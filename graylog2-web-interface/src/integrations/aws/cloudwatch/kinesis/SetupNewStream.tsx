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

import { Button } from 'components/bootstrap';
import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import FormWrap from 'integrations/aws/common/FormWrap';
import { ApiRoutes } from 'integrations/aws/common/Routes';
import { renderOptions } from 'integrations/aws/common/Options';
import useFetch from 'integrations/aws/common/hooks/useFetch';
import formValidation from 'integrations/aws/utils/formValidation';
import { FormDataContext } from 'integrations/aws/context/FormData';
import { ApiContext } from 'integrations/aws/context/Api';

import SetupModal from './setup-steps/SetupModal';

type KinesisSetupProps = {
  onSubmit: (...args: any[]) => void;
  onChange: (...args: any[]) => void;
  toggleSetup?: (...args: any[]) => void;
};

const KinesisSetup = ({
  onChange,
  onSubmit,
  toggleSetup = null,
}: KinesisSetupProps) => {
  const { availableGroups, setGroups, clearLogData } = useContext(ApiContext);
  const { formData } = useContext(FormDataContext);
  const [formError, setFormError] = useState(null);
  const [disabledForm, setDisabledForm] = useState(false);
  const [disabledGroups, setDisabledGroups] = useState(false);
  const [showTOS, setShowTOS] = useState(false);
  const [groupNamesStatus, setGroupNamesUrl] = useFetch(
    ApiRoutes.INTEGRATIONS.AWS.CLOUDWATCH.GROUPS,
    (response) => {
      setGroups(response);
    },
    'POST',
    { region: formData.awsCloudWatchAwsRegion.value },
  );

  useEffect(() => {
    if (groupNamesStatus.error) {
      setGroupNamesUrl(null);

      const noGroups = /No CloudWatch log groups/g;

      if (groupNamesStatus.error.match(noGroups)) {
        setFormError({
          full_message: groupNamesStatus.error,
          nice_message: <span>We&apos;re unable to find any groups in your chosen region. Please try selecting a different region.</span>,
        });

        setDisabledGroups(true);
      } else {
        setFormError({
          full_message: groupNamesStatus.error,
        });
      }
    }

    return () => {
      setGroups({ log_groups: [] });
    };
  }, [groupNamesStatus.error]);

  const handleAgreeSubmit = () => {
    clearLogData();
    onSubmit();
  };

  const handleFormSubmit = () => {
    setDisabledForm(true);
    setShowTOS(true);
  };

  const handleAgreeCancel = () => {
    setDisabledForm(false);
    setShowTOS(false);
  };

  return (
    <FormWrap onSubmit={handleFormSubmit}
              buttonContent="Begin Automated Setup"
              disabled={formValidation.isFormValid([
                'awsCloudWatchKinesisStream',
                'awsCloudWatchAwsGroupName',
              ], formData) || disabledForm}
              loading={groupNamesStatus.loading}
              error={formError}
              title="Set Up Kinesis Automatically"
              description="">

      <p>
        Complete the fields below and Graylog will perform the automated Kinesis setup, which performs the following operations within your AWS account. See <a target="_blank" rel="noopener noreferrer" href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html">Using CloudWatch Logs Subscription Filters</a> in the AWS documentation for more information.
      </p>

      <ol>
        <li>Create a new Kinesis stream with the specified name.</li>
        <li>Create the IAM role/policy needed to subscribe the Kinesis stream to the CloudWatch Log Group.</li>
        <li>Subscribe the new Kinesis stream to the Log Group.</li>
      </ol>

      <ValidatedInput id="awsCloudWatchKinesisStream"
                      type="text"
                      label="Kinesis Stream Name"
                      placeholder="Stream Name"
                      onChange={onChange}
                      fieldData={formData.awsCloudWatchKinesisStream}
                      disabled={disabledForm}
                      pattern="[a-zA-Z0-9_.-]{1,128}$"
                      help="1-128 alphanumeric characters and special characters underscore (_), period (.), and hyphen (-)."
                      required />

      <ValidatedInput id="awsCloudWatchAwsGroupName"
                      type="select"
                      fieldData={formData.awsCloudWatchAwsGroupName}
                      onChange={onChange}
                      label="CloudWatch Group Name"
                      required
                      disabled={groupNamesStatus.loading || disabledGroups || disabledForm}>

        {renderOptions(availableGroups, 'Choose CloudWatch Group', groupNamesStatus.loading)}
      </ValidatedInput>

      {toggleSetup
        && (
        <BackButton onClick={toggleSetup}
                    type="button"
                    disabled={disabledForm}>
          Back to stream Selection
        </BackButton>
        )}

      {showTOS && (
      <SetupModal onSubmit={handleAgreeSubmit}
                  onCancel={handleAgreeCancel}
                  groupName={formData.awsCloudWatchAwsGroupName.value}
                  streamName={formData.awsCloudWatchKinesisStream.value} />
      )}
    </FormWrap>
  );
};

const BackButton = styled(Button)`
  margin-right: 9px;
`;

export default KinesisSetup;
