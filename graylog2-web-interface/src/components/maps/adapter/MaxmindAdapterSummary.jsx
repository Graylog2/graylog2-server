import PropTypes from 'prop-types';
import React from 'react';
import { TimeUnit } from 'components/common';

class MaxmindAdapterSummary extends React.Component {
  static propTypes = {
    dataAdapter: PropTypes.object.isRequired,
  };

  render() {
    const config = this.props.dataAdapter.config;
    const databaseTypes = {
      MAXMIND_CITY: 'City database',
      MAXMIND_COUNTRY: 'Country database',
    };
    return (<dl>
      <dt>Database file path</dt>
      <dd>{config.path}</dd>
      <dt>Database type</dt>
      <dd>{databaseTypes[config.database_type]}</dd>
      <dt>Check interval</dt>
      <dd><TimeUnit value={config.check_interval} unit={config.check_interval_unit} /></dd>
    </dl>);
  }
}

export default MaxmindAdapterSummary;
