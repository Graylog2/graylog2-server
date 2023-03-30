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
import PropTypes from 'prop-types';

class OTXAdapterSummary extends React.Component {
  static propTypes = {
    dataAdapter: PropTypes.shape({
      config: PropTypes.shape({
        indicator: PropTypes.string.isRequired,
        api_key: PropTypes.string,
        api_url: PropTypes.string.isRequired,
        http_user_agent: PropTypes.string.isRequired,
        http_connect_timeout: PropTypes.number.isRequired,
        http_write_timeout: PropTypes.number.isRequired,
        http_read_timeout: PropTypes.number.isRequired,
      }),
    }),
  };

  render() {
    const { config } = this.props.dataAdapter;

    return (
      <dl>
        <dt>Indicator</dt>
        <dd>{config.indicator}</dd>
        <dt>OTX API Key</dt>
        <dd>{config.api_key || 'n/a'}</dd>
        <dt>OTX API URL</dt>
        <dd>{config.api_url}</dd>
        <dt>HTTP User-Agent</dt>
        <dd>{config.http_user_agent}</dd>
        <dt>HTTP Connect Timeout</dt>
        <dd>{config.http_connect_timeout} ms</dd>
        <dt>HTTP Write Timeout</dt>
        <dd>{config.http_write_timeout} ms</dd>
        <dt>HTTP Read Timeout</dt>
        <dd>{config.http_read_timeout} ms</dd>
      </dl>
    );
  }
}

export default OTXAdapterSummary;
