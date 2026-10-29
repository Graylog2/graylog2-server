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
import { useTranslation } from 'react-i18next';

type OTXAdapterSummaryProps = {
  dataAdapter: {
    config: {
      indicator: string;
      api_key?: string;
      api_url: string;
      http_user_agent: string;
      http_connect_timeout: number;
      http_write_timeout: number;
      http_read_timeout: number;
    };
  };
};

const OTXAdapterSummary = ({ dataAdapter: { config } }: OTXAdapterSummaryProps) => {
  const { t } = useTranslation('integrations');

  return (
    <dl>
      <dt>{t('otx_adapter_field_set.indicator_label')}</dt>
      <dd>{config.indicator}</dd>
      <dt>{t('otx_adapter_field_set.api_key_label')}</dt>
      <dd>{config.api_key || t('otx_adapter_summary.not_available')}</dd>
      <dt>{t('otx_adapter_field_set.api_url_label')}</dt>
      <dd>{config.api_url}</dd>
      <dt>{t('otx_adapter_field_set.user_agent_label')}</dt>
      <dd>{config.http_user_agent}</dd>
      <dt>{t('otx_adapter_field_set.connect_timeout_label')}</dt>
      <dd>
        {config.http_connect_timeout} {t('otx_adapter_summary.unit_ms')}
      </dd>
      <dt>{t('otx_adapter_field_set.write_timeout_label')}</dt>
      <dd>
        {config.http_write_timeout} {t('otx_adapter_summary.unit_ms')}
      </dd>
      <dt>{t('otx_adapter_field_set.read_timeout_label')}</dt>
      <dd>
        {config.http_read_timeout} {t('otx_adapter_summary.unit_ms')}
      </dd>
    </dl>
  );
};

export default OTXAdapterSummary;
