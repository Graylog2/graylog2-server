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
