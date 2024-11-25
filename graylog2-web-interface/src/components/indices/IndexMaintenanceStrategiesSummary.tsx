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

import { Alert } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';
import type {
  RotationStrategyConfig,
  RetentionStrategyConfig,

} from 'components/indices/Types';
import type { IndexRotationConfig } from 'components/indices/rotation/types';

type Props = {
 config: {
  strategy: string,
  config: RotationStrategyConfig | RetentionStrategyConfig
 },
 pluginExports: Array<IndexRotationConfig>,
 rotationStrategyClass?: string
}

const IndexMaintenanceStrategiesSummary = ({ config, pluginExports, rotationStrategyClass } : Props) => {
  if (!config) {
    return (<Spinner />);
  }

  const activeStrategy = config.strategy;
  const strategy = pluginExports.filter((exportedStrategy) => exportedStrategy.type === activeStrategy)[0];

  if (!strategy || !strategy.summaryComponent) {
    return (<Alert bsStyle="danger">Summary for strategy {activeStrategy} not found!</Alert>);
  }

  const componentProps = rotationStrategyClass ? { config: config.config, rotationStrategyClass } : { config: config.config };

  const element = React.createElement(strategy.summaryComponent, componentProps);

  return (<span key={strategy.type}>{element}</span>);
};

export default IndexMaintenanceStrategiesSummary;
