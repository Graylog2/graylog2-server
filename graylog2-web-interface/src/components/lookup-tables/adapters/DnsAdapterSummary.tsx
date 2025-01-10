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

const DnsAdapterSummary = ({
  dataAdapter,
}: DnsAdapterSummaryProps) => {
  const { config } = dataAdapter;

  // Allows enum > display label translation.
  const lookupType = {
    A: 'Resolve hostname to IPv4 address (A)',
    AAAA: 'Resolve hostname to IPv6 address (AAAA)',
    A_AAAA: 'Resolve hostname to IPv4 and IPv6 address (A and AAAA)',
    PTR: 'Reverse lookup (PTR)',
    TXT: 'Text lookup (TXT)',
  };

  return (
    <dl>
      <dt>DNS Lookup Type</dt>
      <dd>{lookupType[config.lookup_type]}</dd>

      <dt>DNS Server IP Address</dt>
      <dd>{config.server_ips || 'n/a'}</dd>

      <dt>DNS Request Timeout</dt>
      <dd>{config.request_timeout} ms</dd>

      <dt>Cache TTL Override</dt>
      <dd>
        {!config.cache_ttl_override_enabled ? 'n/a' : <TimeUnit value={config.cache_ttl_override} unit={config.cache_ttl_override_unit} />}
      </dd>
    </dl>
  );
};

export default DnsAdapterSummary;
