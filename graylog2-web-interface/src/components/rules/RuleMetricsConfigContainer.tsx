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

import connect from 'stores/connect';
import { RulesActions, RulesStore } from 'stores/rules/RulesStore';

import RuleMetricsConfig from './RuleMetricsConfig';

const handleChange = (nextConfig) => RulesActions.updateMetricsConfig(nextConfig);

type RuleMetricsConfigContainerProps = {
  metricsConfig?: any;
  onClose?: (...args: any[]) => void;
};

class RuleMetricsConfigContainer extends React.Component<RuleMetricsConfigContainerProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    metricsConfig: undefined,
    onClose: () => {},
  };

  componentDidMount() {
    RulesActions.loadMetricsConfig();
  }

  render() {
    const { metricsConfig, onClose } = this.props;

    if (!metricsConfig) {
      return null;
    }

    return (
      <RuleMetricsConfig config={metricsConfig}
                         onChange={handleChange}
                         onClose={onClose} />
    );
  }
}

export default connect(RuleMetricsConfigContainer, { rules: RulesStore }, ({ rules }) => ({ metricsConfig: rules ? rules.metricsConfig : rules }));
