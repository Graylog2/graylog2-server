import React from 'react';
import PropTypes from 'prop-types';

import { TimeUnit } from 'components/common';

const DnsAdapterSummary = ({ dataAdapter }) => {
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
      <dd>{ lookupType[config.lookup_type] }</dd>

      <dt>DNS Server IP Address</dt>
      <dd>{ config.server_ips || 'n/a' }</dd>

      <dt>DNS Request Timeout</dt>
      <dd>{ config.request_timeout } ms</dd>

      <dt>Cache TTL Override</dt>
      <dd>
        { !config.cache_ttl_override_enabled ? 'n/a' : <TimeUnit value={config.cache_ttl_override} unit={config.cache_ttl_override_unit} /> }
      </dd>
    </dl>
  );
};

DnsAdapterSummary.propTypes = {
  dataAdapter: PropTypes.shape({
    config: PropTypes.shape({
      lookup_type: PropTypes.string.isRequired,
      request_timeout: PropTypes.number.isRequired,
    }),
  }).isRequired,
};

export default DnsAdapterSummary;
