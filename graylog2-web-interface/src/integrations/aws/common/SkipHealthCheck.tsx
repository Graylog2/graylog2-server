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

import FormWrap from 'integrations/aws/common/FormWrap';
import AdditionalFields from 'integrations/aws/common/AdditionalFields';
import { renderOptions } from 'integrations/aws/common/Options';
import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import { KINESIS_LOG_TYPES } from 'integrations/aws/common/constants';
import FormDataContext from 'integrations/contexts/FormDataContext';

const StyledFormWrap = styled(FormWrap)`
  padding-top: 25px;
`;

type SkipHealthCheckProps = {
  onSubmit: (...args: any[]) => void;
  onChange: (...args: any[]) => void;
};

const SkipHealthCheck = ({ onChange, onSubmit }: SkipHealthCheckProps) => {
  const { t } = useTranslation('integrations');
  const { formData } = useContext(FormDataContext);

  return (
    <AdditionalFields title={t('aws_skip_health_check.title')}>
      <StyledFormWrap
        onSubmit={onSubmit}
        buttonContent={t('aws_skip_health_check.confirm_button')}
        title={t('aws_skip_health_check.form_title')}
        disabled={!(formData.awsCloudWatchKinesisInputType && formData.awsCloudWatchKinesisInputType.value)}
        description={
          <p>
            <Trans
              t={t}
              i18nKey="aws_skip_health_check.description"
              values={{ streamName: formData.awsCloudWatchKinesisStream.value }}
              components={{ strong: <strong /> }}
            />
          </p>
        }>
        <ValidatedInput
          id="awsCloudWatchKinesisInputType"
          type="select"
          fieldData={formData.awsCloudWatchKinesisInputType}
          onChange={onChange}
          label={t('aws_skip_health_check.input_type_label')}
          required>
          {renderOptions(KINESIS_LOG_TYPES, t('aws_skip_health_check.choose_log_type'))}
        </ValidatedInput>
      </StyledFormWrap>
    </AdditionalFields>
  );
};

export default SkipHealthCheck;
