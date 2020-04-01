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
