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
import { Trans, useTranslation } from 'react-i18next';

import { Button } from 'components/bootstrap';
import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import FormWrap from 'integrations/aws/common/FormWrap';
import { ApiRoutes } from 'integrations/aws/common/Routes';
import { renderOptions } from 'integrations/aws/common/Options';
import useFetch from 'integrations/hooks/useFetch';
import formValidation from 'integrations/aws/utils/formValidation';
import FormDataContext from 'integrations/contexts/FormDataContext';
import { ApiContext } from 'integrations/aws/context/Api';
import { toAWSRequest } from 'integrations/aws/common/formDataAdapter';

import SetupModal from './setup-steps/SetupModal';

const BackButton = styled(Button)`
  margin-right: 9px;
`;

const DocsAnchor = ({ href, children }: { href: string; children?: React.ReactNode }) => (
  <a href={href} target="_blank" rel="noopener noreferrer">
    {children}
  </a>
);

type KinesisSetupProps = {
  onSubmit: (...args: any[]) => void;
  onChange: (...args: any[]) => void;
  toggleSetup?: (...args: any[]) => void;
};

const KinesisSetup = ({ onChange, onSubmit, toggleSetup = null }: KinesisSetupProps) => {
  const { t } = useTranslation('integrations');
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
    toAWSRequest(formData, { region: formData.awsCloudWatchAwsRegion.value }),
  );

  useEffect(() => {
    if (groupNamesStatus.error) {
      setGroupNamesUrl(null);

      const noGroups = /No CloudWatch log groups/g;

      if (groupNamesStatus.error.match(noGroups)) {
        setFormError({
          full_message: groupNamesStatus.error,
          nice_message: <span>{t('aws_kinesis_setup_new_stream.no_groups_error')}</span>,
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
  }, [groupNamesStatus.error, setGroupNamesUrl, setGroups, t]);

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
    <FormWrap
      onSubmit={handleFormSubmit}
      buttonContent={t('aws_kinesis_setup_new_stream.begin_button')}
      disabled={
        formValidation.isFormValid(['awsCloudWatchKinesisStream', 'awsCloudWatchAwsGroupName'], formData) ||
        disabledForm
      }
      loading={groupNamesStatus.loading}
      error={formError}
      title={t('aws_kinesis_setup_new_stream.title')}
      description="">
      <p>
        <Trans
          t={t}
          i18nKey="aws_kinesis_setup_new_stream.intro"
          components={{
            docsLink: (
              <DocsAnchor href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html" />
            ),
          }}
        />
      </p>

      <ol>
        <li>{t('aws_kinesis_setup_new_stream.step_create_stream')}</li>
        <li>{t('aws_kinesis_setup_new_stream.step_create_role')}</li>
        <li>{t('aws_kinesis_setup_new_stream.step_subscribe')}</li>
      </ol>

      <ValidatedInput
        id="awsCloudWatchKinesisStream"
        type="text"
        label={t('aws_kinesis_setup_new_stream.stream_name_label')}
        placeholder={t('aws_kinesis_setup_new_stream.stream_name_placeholder')}
        onChange={onChange}
        fieldData={formData.awsCloudWatchKinesisStream}
        disabled={disabledForm}
        pattern="[a-zA-Z0-9_.-]{1,128}$"
        help={t('aws_kinesis_setup_new_stream.stream_name_help')}
        required
      />

      <ValidatedInput
        id="awsCloudWatchAwsGroupName"
        type="select"
        fieldData={formData.awsCloudWatchAwsGroupName}
        onChange={onChange}
        label={t('aws_kinesis_setup_new_stream.group_name_label')}
        required
        disabled={groupNamesStatus.loading || disabledGroups || disabledForm}>
        {renderOptions(availableGroups, t('aws_kinesis_setup_new_stream.choose_group'), groupNamesStatus.loading)}
      </ValidatedInput>

      {toggleSetup && (
        <BackButton onClick={toggleSetup} type="button" disabled={disabledForm}>
          {t('aws_kinesis_setup_new_stream.back_button')}
        </BackButton>
      )}

      {showTOS && (
        <SetupModal
          onSubmit={handleAgreeSubmit}
          onCancel={handleAgreeCancel}
          groupName={formData.awsCloudWatchAwsGroupName.value}
          streamName={formData.awsCloudWatchKinesisStream.value}
        />
      )}
    </FormWrap>
  );
};

export default KinesisSetup;
