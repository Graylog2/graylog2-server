import React from 'react';
import Reflux from 'reflux';
import { Col } from 'react-bootstrap';
import String from 'string';
import numeral from 'numeral';

import MetricsActions from 'actions/metrics/MetricsActions';
import MetricsStore from 'stores/metrics/MetricsStore';

import { Spinner } from 'components/common';

const LogLevelMetrics = React.createClass({
  propTypes: {
    nodeId: React.PropTypes.string.isRequired,
    loglevel: React.PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(MetricsStore)],
  componentDidMount() {
    MetricsActions.add(this.props.nodeId, this._metricName());
  },
  componentWillUnmount() {
    MetricsActions.remove(this.props.nodeId, this._metricName());
  },
  _metricName() {
    return 'org.apache.logging.log4j.core.Appender.' + this.props.loglevel;
  },
  render() {
    const { loglevel, nodeId } = this.props;
    const { metrics } = this.state;
    if (!metrics || !metrics[nodeId] || !metrics[nodeId][this._metricName()]) {
      return <Spinner />;
    }
    const metric = metrics[nodeId][this._metricName()].metric;
    return (
      <div className="loglevel-metrics-row">
        <Col md={4}>
          <h3 className="u-light">Level: {String(loglevel).capitalize().toString()}</h3>
          <dl className="loglevel-metrics-list">
            <dt>Total written:</dt>
            <dd><span className="loglevel-metric-total">{metric.rate.total}</span></dd>
            <dt>Mean rate:</dt>
            <dd><span className="loglevel-metric-mean">{numeral(metric.rate.mean).format('0.00')}</span> / second</dd>
            <dt>1 min rate:</dt>
            <dd><span className="loglevel-metric-1min">{numeral(metric.rate.one_minute).format('0.00')}</span> / second</dd>
          </dl>
        </Col>
      </div>
    );
  },
});

export default LogLevelMetrics;
