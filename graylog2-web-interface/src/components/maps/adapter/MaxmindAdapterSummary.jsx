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
import PropTypes from 'prop-types';
import React from 'react';

import { TimeUnit } from 'components/common';

class MaxmindAdapterSummary extends React.Component {
  static propTypes = {
    dataAdapter: PropTypes.object.isRequired,
  };

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
