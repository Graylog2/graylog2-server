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
import React from 'react';
import styled, { css } from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import { Alert } from 'components/bootstrap';
import { ExternalLink } from 'components/common';

const StyledOl = styled.ol(
  ({ theme }) => css`
    padding-left: 20px;

    > li {
      margin-bottom: ${theme.spacings.sm};
    }

    > li:last-child {
      margin-bottom: 0;
    }
  `,
);

const GCSSetupInfo = () => {
  const { t } = useTranslation('integrations');

  return (
    <Alert bsStyle="info">
      <p>{t('gcs_setup_info.intro')}</p>
      <StyledOl>
        <li>
          <Trans
            t={t}
            i18nKey="gcs_setup_info.step_bucket"
            components={{
              bucketsLink: <ExternalLink href="https://cloud.google.com/storage/docs/creating-buckets" />,
            }}
          />
        </li>
        <li>
          <Trans
            t={t}
            i18nKey="gcs_setup_info.step_service_account"
            components={{
              serviceAccountsLink: <ExternalLink href="https://cloud.google.com/iam/docs/service-account-overview" />,
            }}
          />
        </li>
        <li>
          <Trans
            t={t}
            i18nKey="gcs_setup_info.step_credentials"
            components={{
              adcLink: <ExternalLink href="https://cloud.google.com/docs/authentication/provide-credentials-adc#how-to" />,
            }}
          />
        </li>
        <li>{t('gcs_setup_info.step_setup_backend')}</li>
      </StyledOl>
    </Alert>
  );
};

export default GCSSetupInfo;
