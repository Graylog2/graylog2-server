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

import { TimeUnit } from 'components/common';
import type { TimeUnit as TimeUnitString } from 'components/common/types';

type DnsAdapterSummaryProps = {
  dataAdapter: {
    config?: {
      lookup_type: string;
      request_timeout: number;
      server_ips: string;
      cache_ttl_override_enabled: boolean;
      cache_ttl_override: number;
      cache_ttl_override_unit: TimeUnitString;
    };
  };
};

const DnsAdapterSummary = ({ dataAdapter }: DnsAdapterSummaryProps) => {
  const { t } = useTranslation('lookup_tables');
  const { config } = dataAdapter;

  // Allows enum > display label translation.
  const lookupType = {
    A: t('dns_adapter.lookup_type_a'),
    AAAA: t('dns_adapter.lookup_type_aaaa'),
    A_AAAA: t('dns_adapter.lookup_type_a_aaaa_singular'),
    PTR: t('dns_adapter.lookup_type_ptr'),
    TXT: t('dns_adapter.lookup_type_txt'),
  };

  return (
    <dl>
      <dt>{t('dns_adapter.lookup_type_label')}</dt>
      <dd>{lookupType[config.lookup_type]}</dd>

      <dt>{t('dns_adapter.server_ips_label')}</dt>
      <dd>{config.server_ips || t('dns_adapter.not_available')}</dd>

      <dt>{t('dns_adapter.request_timeout_label')}</dt>
      <dd>{t('dns_adapter.timeout_ms', { value: config.request_timeout })}</dd>

      <dt>{t('dns_adapter.cache_ttl_override')}</dt>
      <dd>
        {!config.cache_ttl_override_enabled ? (
          t('dns_adapter.not_available')
        ) : (
          <TimeUnit value={config.cache_ttl_override} unit={config.cache_ttl_override_unit} />
        )}
      </dd>
    </dl>
  );
};

export default DnsAdapterSummary;
