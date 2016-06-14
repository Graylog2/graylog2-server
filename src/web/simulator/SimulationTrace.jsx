import React from 'react';

import NumberUtils from 'util/NumberUtils';

const SimulationTrace = React.createClass({
  propTypes: {
    simulationResults: React.PropTypes.object.isRequired,
  },

  componentDidMount() {
    this.style.use();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  style: require('!style/useable!css!./SimulationTrace.css'),

  render() {
    const simulationTrace = this.props.simulationResults.simulation_trace;

    const traceEntries = [];

    simulationTrace.forEach((trace, idx) => {
      traceEntries.push(<dt key={`${trace.time}-${idx}-title`}>{NumberUtils.formatNumber(trace.time)} &#956;s</dt>);
      traceEntries.push(<dd key={`${trace}-${idx}-description`}><span>{trace.message}</span></dd>);
    });

    return (
      <dl className="dl-horizontal dl-simulation-trace">
        {traceEntries}
      </dl>
    );
  },
});

export default SimulationTrace;
