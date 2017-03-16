import React from 'react';
import { Alert } from 'react-bootstrap';
import Spinner from 'components/common/Spinner';

const IndexMaintenanceStrategiesSummary = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
    pluginExports: React.PropTypes.array.isRequired,
  },

  render() {
    if (!this.props.config) {
      return (<Spinner />);
    }

    const activeStrategy = this.props.config.strategy;
    const strategy = this.props.pluginExports.filter(exportedStrategy => exportedStrategy.type === activeStrategy)[0];

    if (!strategy || !strategy.summaryComponent) {
      return (<Alert bsStyle="danger">Summary for strategy {activeStrategy} not found!</Alert>);
    }

    const element = React.createElement(strategy.summaryComponent, { config: this.props.config.config });

    return (<span key={strategy.type}>{element}</span>);
  },
});

export default IndexMaintenanceStrategiesSummary;
