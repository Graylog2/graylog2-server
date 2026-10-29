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
import { Trans, useTranslation } from 'react-i18next';

import PageHeader from 'components/common/PageHeader';
import { ExternalLink } from 'components/common';

import EmbeddedCloudTrailApp from './EmbeddedCloudTrailApp';

const CloudTrailApp = () => {
  const { t } = useTranslation('integrations');

  return (
    <>
      <PageHeader title={t('aws_cloudtrail_app.title')}>
        <span>{t('aws_cloudtrail_app.description')}</span>
        <p>
          <Trans
            t={t}
            i18nKey="aws_cloudtrail_app.requirement"
            components={{
              cloudTrailLink: <ExternalLink href="https://aws.amazon.com/console/" />,
            }}
          />
        </p>
      </PageHeader>
      <EmbeddedCloudTrailApp />
    </>
  );
};

export default CloudTrailApp;
