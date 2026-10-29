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
/* eslint-disable react/no-unescaped-entities */
import React from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { ExternalLink } from 'components/common';

const style: React.CSSProperties = { marginBottom: 10 };

const OTXAdapterDocumentation = (): React.ReactElement => {
  const { t } = useTranslation('integrations');

  return (
    <div>
      <p style={style}>
        <Trans
          t={t}
          i18nKey="otx_adapter_documentation.intro"
          components={{ otxLink: <ExternalLink href="https://otx.alienvault.com/api" /> }}
        />
      </p>

      <h3 style={style}>{t('otx_adapter_documentation.configuration_title')}</h3>

      <h5 style={style}>{t('otx_adapter_documentation.indicator_title')}</h5>

      <p style={style}>{t('otx_adapter_documentation.indicator_description')}</p>
      <p style={style}>
        <Trans
          t={t}
          i18nKey="otx_adapter_documentation.ip_auto_detect"
          components={{ code: <code /> }}
        />
      </p>

      <h5 style={style}>{t('otx_adapter_documentation.api_key_title')}</h5>

      <p style={style}>
        <Trans
          t={t}
          i18nKey="otx_adapter_documentation.api_key_description"
          components={{ strong: <strong /> }}
        />
      </p>

      <h5 style={style}>{t('otx_adapter_documentation.api_url_title')}</h5>

      <p style={style}>
        <Trans t={t} i18nKey="otx_adapter_documentation.api_url_description" components={{ code: <code /> }} />
      </p>

      <h5 style={style}>{t('otx_adapter_documentation.user_agent_title')}</h5>

      <p style={style}>
        <Trans t={t} i18nKey="otx_adapter_documentation.user_agent_description" components={{ code: <code /> }} />
      </p>

      <h5 style={style}>{t('otx_adapter_documentation.connection_timeout_title')}</h5>

      <p style={style}>{t('otx_adapter_documentation.connection_timeout_description')}</p>

      <h5 style={style}>{t('otx_adapter_documentation.write_timeout_title')}</h5>

      <p style={style}>{t('otx_adapter_documentation.write_timeout_description')}</p>

      <h5 style={style}>{t('otx_adapter_documentation.read_timeout_title')}</h5>

      <p style={style}>{t('otx_adapter_documentation.read_timeout_description')}</p>
    </div>
  );
};

export default OTXAdapterDocumentation;
