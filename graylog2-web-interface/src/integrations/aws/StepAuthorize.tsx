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
import React, { useContext, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import FormDataContext from 'integrations/contexts/FormDataContext';
import { ApiContext } from 'integrations/aws/context/Api';
import { SidebarContext } from 'integrations/aws/context/Sidebar';
import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import FormWrap from 'integrations/aws/common/FormWrap';
import { renderOptions } from 'integrations/aws/common/Options';
import { ApiRoutes } from 'integrations/aws/common/Routes';
import { AWS_AUTH_TYPES } from 'integrations/aws/common/constants';
import useFetch from 'integrations/hooks/useFetch';
import formValidation from 'integrations/aws/utils/formValidation';
import { validateExternalIdRequiresArn } from 'integrations/aws/utils/awsValidation';
import AWSAuthenticationTypes from 'integrations/aws/authentication/AWSAuthenticationTypes';
import AWSCustomEndpoints from 'integrations/aws/authentication/AWSCustomEndpoints';
import { toAWSRequest } from 'integrations/aws/common/formDataAdapter';

const DisappearingInput = styled.input`
  position: fixed;
  top: -500vh;
  left: -500vw;
`;

type StepAuthorizeProps = {
  onSubmit: (...args: any[]) => void;
  onChange: (...args: any[]) => void;
  sidebarComponent?: React.ReactNode;
};

const StepAuthorize = ({ onChange, onSubmit, sidebarComponent = null }: StepAuthorizeProps) => {
  const { t } = useTranslation('integrations');
  const { formData } = useContext(FormDataContext);
  const { clearSidebar, setSidebar } = useContext(SidebarContext);
  const { availableRegions, setRegions, setStreams } = useContext(ApiContext);
  const [externalIdError, setExternalIdError] = useState<string | null>(null);
  const [fetchRegionsStatus] = useFetch(ApiRoutes.INTEGRATIONS.AWS.REGIONS, setRegions, 'GET');
  const [fetchStreamsStatus, setStreamsFetch] = useFetch(
    null,
    (response) => {
      setStreams(response);
      onSubmit();
    },
    'POST',
    toAWSRequest(formData, { region: formData.awsCloudWatchAwsRegion ? formData.awsCloudWatchAwsRegion.value : '' }),
  );

  // Derive the fetch-related error during render instead of syncing it via setState in an effect.
  const fetchError = useMemo(() => {
    if (fetchRegionsStatus.error) {
      return { full_message: fetchRegionsStatus.error };
    }

    if (fetchStreamsStatus.error) {
      const badKey = /security token/g;
      const badSecret = /signing method/g;

      if (fetchStreamsStatus.error.match(badKey)) {
        return {
          full_message: fetchStreamsStatus.error,
          nice_message: t('aws_step_authorize.invalid_key_message'),
        };
      }

      if (fetchStreamsStatus.error.match(badSecret)) {
        return {
          full_message: fetchStreamsStatus.error,
          nice_message: t('aws_step_authorize.invalid_secret_message'),
        };
      }

      return { full_message: fetchStreamsStatus.error };
    }

    return null;
  }, [fetchRegionsStatus.error, fetchStreamsStatus.error, t]);

  // Side-effect only: navigate forward when streams returns empty (no setState here).
  useEffect(() => {
    const noStreams = /No Kinesis streams/g;

    if (fetchStreamsStatus.error?.match(noStreams)) {
      setStreams({ streams: [] });
      onSubmit();
    }
  }, [fetchStreamsStatus.error, onSubmit, setStreams]);

  // Reset the fetch trigger on mount/unmount.
  useEffect(() => {
    setStreamsFetch(null);

    return () => {
      setStreamsFetch(null);
    };
  }, [setStreamsFetch]);

  const handleSubmit = () => {
    const arnError = validateExternalIdRequiresArn(formData?.awsAssumeRoleARN?.value, formData?.awsExternalId?.value);

    if (arnError) {
      setExternalIdError(arnError);

      return;
    }

    setExternalIdError(null);
    setStreamsFetch(ApiRoutes.INTEGRATIONS.AWS.KINESIS.STREAMS);
  };

  useEffect(() => {
    if (sidebarComponent) {
      setSidebar(sidebarComponent);
    }

    return () => {
      clearSidebar();
    };
  }, [clearSidebar, setSidebar, sidebarComponent]);

  const formError = externalIdError ? { full_message: externalIdError, nice_message: externalIdError } : fetchError;

  const authType = formData.awsAuthenticationType && formData.awsAuthenticationType.value;
  const isFormValid = formValidation.isFormValid(
    [
      'awsCloudWatchName',
      'awsCloudWatchAwsRegion',
      ...(authType !== AWS_AUTH_TYPES.automatic ? ['awsAccessKey', 'awsSecretKey'] : []),
    ],
    formData,
  );

  return (
    <FormWrap
      onSubmit={handleSubmit}
      buttonContent={t('aws_step_authorize.authorize_button')}
      loading={fetchRegionsStatus.loading || fetchStreamsStatus.loading}
      disabled={isFormValid}
      error={formError}
      title={t('aws_step_authorize.title')}
      description={t('aws_step_authorize.description')}>
      <DisappearingInput id="name" type="text" />
      <DisappearingInput id="password" type="password" />

      <ValidatedInput
        id="awsCloudWatchName"
        type="text"
        fieldData={formData.awsCloudWatchName}
        onChange={onChange}
        placeholder={t('aws_step_authorize.input_name')}
        label={t('aws_step_authorize.input_name')}
        autoComplete="off"
        required
      />

      <AWSAuthenticationTypes onChange={onChange} />

      <ValidatedInput
        id="awsCloudWatchAwsRegion"
        type="select"
        fieldData={formData.awsCloudWatchAwsRegion}
        onChange={onChange}
        label={t('aws_step_authorize.aws_region')}
        help={t('aws_step_authorize.aws_region_help')}
        disabled={fetchRegionsStatus.loading}
        required>
        {renderOptions(availableRegions, t('aws_step_authorize.choose_region'), fetchRegionsStatus.loading)}
      </ValidatedInput>

      <AWSCustomEndpoints onChange={onChange} />
    </FormWrap>
  );
};

export default StepAuthorize;
