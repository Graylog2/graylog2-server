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

type WhoisAdapterSummaryProps = {
  dataAdapter: {
    config?: {
      connect_timeout: number;
      read_timeout: number;
    };
  };
};

const WhoisAdapterSummary = ({ dataAdapter }: WhoisAdapterSummaryProps) => {
  const { t } = useTranslation('integrations');
  const { config } = dataAdapter;

  return (
    <dl>
      <dt>{t('whois_adapter_field_set.connect_timeout_label')}</dt>
      <dd>
        {config.connect_timeout} {t('whois_adapter_summary.unit_ms')}
      </dd>
      <dt>{t('whois_adapter_field_set.read_timeout_label')}</dt>
      <dd>
        {config.read_timeout} {t('whois_adapter_summary.unit_ms')}
      </dd>
    </dl>
  );
};

export default WhoisAdapterSummary;
