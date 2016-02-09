import React from 'react';

import { MetricDetails } from 'components/metrics';

const Metric = React.createClass({
  propTypes: {
    metric: React.PropTypes.object.isRequired,
    namespace: React.PropTypes.string,
    nodeId: React.PropTypes.string.isRequired,
  },
  getInitialState() {
    return {
      expanded: false,
    };
  },
  iconMapping: {
    timer: 'clock-o',
    histogram: 'signal',
    meter: 'play-circle',
    gauge: 'dashboard',
    counter: 'circle',
    unknown: 'question-circle',
  },
  _formatIcon(type) {
    const icon = this.iconMapping[type];
    if (icon) {
      return icon;
    }

    return this.iconMapping.unknown;
  },
  _formatName(metricName) {
    return (
      <span>
        <span className="prefix">{this.props.namespace}</span>
        {metricName.split(this.props.namespace)[1]}
      </span>
    );
  },
  _showDetails(event) {
    event.preventDefault();
    this.setState({ expanded: !this.state.expanded });
  },
  render() {
    const metric = this.props.metric;
    const details = this.state.expanded ? <MetricDetails nodeId={this.props.nodeId} metric={this.props.metric} /> : null;
    return (
      <span>
        <div className="name">
          <i className={'fa fa-' + this._formatIcon(metric.type)} />{' '}
          <a className="open" href="#" onClick={this._showDetails}>{this._formatName(metric.full_name)}</a>
        </div>
        {details}
      </span>
    );
  },
});

export default Metric;
