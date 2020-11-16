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

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import RuleMetricsConfig from './RuleMetricsConfig';

const { RulesStore, RulesActions } = CombinedProvider.get('Rules');

class RuleMetricsConfigContainer extends React.Component {
  static propTypes = {
    metricsConfig: PropTypes.object,
    onClose: PropTypes.func,
  };

  static defaultProps = {
    metricsConfig: undefined,
    onClose: () => {},
  };

  componentDidMount() {
    RulesActions.loadMetricsConfig();
  }

  handleChange = (nextConfig) => {
    return RulesActions.updateMetricsConfig(nextConfig);
  };

  render() {
    const { metricsConfig, onClose } = this.props;

    if (!metricsConfig) {
      return null;
    }

    return (
      <RuleMetricsConfig ref={(component) => { this.configComponent = component; }}
                         config={metricsConfig}
                         onChange={this.handleChange}
                         onClose={onClose} />
    );
  }
}

export default connect(RuleMetricsConfigContainer, { rules: RulesStore }, ({ rules }) => {
  return { metricsConfig: rules ? rules.metricsConfig : rules };
});
