import React from 'react';

import { TimeUnit } from 'components/common';

type MaxmindAdapterSummaryProps = {
  dataAdapter: any;
};

class MaxmindAdapterSummary extends React.Component<MaxmindAdapterSummaryProps, {
  [key: string]: any;
}> {
  render() {
    const { config } = this.props.dataAdapter;
    const databaseTypes = {
      MAXMIND_ASN: 'ASN database',
      MAXMIND_CITY: 'City database',
      MAXMIND_COUNTRY: 'Country database',
      IPINFO_STANDARD_LOCATION: 'IPinfo location database',
      IPINFO_ASN: 'IPinfo ASN database',
    };

    return (
      <dl>
        <dt>Database file path</dt>
        <dd>{config.path}</dd>
        <dt>Database type</dt>
        <dd>{databaseTypes[config.database_type]}</dd>
        <dt>Check interval</dt>
        <dd><TimeUnit value={config.check_interval} unit={config.check_interval_unit} /></dd>
      </dl>
    );
  }
}

export default MaxmindAdapterSummary;
