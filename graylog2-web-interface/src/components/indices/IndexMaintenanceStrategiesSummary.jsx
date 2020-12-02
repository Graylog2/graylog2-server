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

import { Alert } from 'components/graylog';
import Spinner from 'components/common/Spinner';

class IndexMaintenanceStrategiesSummary extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    pluginExports: PropTypes.array.isRequired,
  };

  render() {
    if (!this.props.config) {
      return (<Spinner />);
    }

    const activeStrategy = this.props.config.strategy;
    const strategy = this.props.pluginExports.filter((exportedStrategy) => exportedStrategy.type === activeStrategy)[0];

    if (!strategy || !strategy.summaryComponent) {
      return (<Alert bsStyle="danger">Summary for strategy {activeStrategy} not found!</Alert>);
    }

    const element = React.createElement(strategy.summaryComponent, { config: this.props.config.config });

    return (<span key={strategy.type}>{element}</span>);
  }
}

export default IndexMaintenanceStrategiesSummary;
