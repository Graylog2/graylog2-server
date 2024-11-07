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
import { Alert } from 'components/bootstrap';

type AbuseChRansomAdapterSummaryProps = {
  dataAdapter: any;
};

class AbuseChRansomAdapterSummary extends React.Component<AbuseChRansomAdapterSummaryProps, {
  [key: string]: any;
}> {
  render() {
    const { config } = this.props.dataAdapter;
    const blocklistType = {
      DOMAINS: 'Domain blocklist',
      URLS: 'URL blocklist',
      IPS: 'IP blocklist',
    };

    return (
      <div>
        <dl>
          <dt>Blocklist type</dt>
          <dd>{blocklistType[config.blocklist_type]}</dd>
          <dt>Update interval</dt>
          <dd><TimeUnit value={config.refresh_interval} unit={config.refresh_interval_unit} /></dd>
        </dl>
        <Alert style={{ marginBottom: 10 }} bsStyle="warning" title="Deprecation Warning">
          <p>The abuse.ch Ransomware Tracker was shut down on 2019-12-08. This Data Adapter should not be used.</p>
        </Alert>
      </div>
    );
  }
}

export default AbuseChRansomAdapterSummary;
