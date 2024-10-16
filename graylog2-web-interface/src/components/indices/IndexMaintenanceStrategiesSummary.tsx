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
