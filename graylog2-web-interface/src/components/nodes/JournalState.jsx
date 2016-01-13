import React, {PropTypes} from 'react';
import MetricsStore from 'stores/metrics/MetricsStore';
import numeral from 'numeral';

const metricsStore = MetricsStore.instance;

const JournalState = React.createClass({
  propTypes: {
    nodeId: PropTypes.string.isRequired,
  },
  getInitialState() {
    return ({
      initialized: false,
      hasError: false,
      append: 0,
      read: 0,
      segments: 0,
      entriesUncommitted: 0,
    });
  },

  componentWillMount() {
    const metricNames = [
      'org.graylog2.journal.append.1-sec-rate',
      'org.graylog2.journal.read.1-sec-rate',
      'org.graylog2.journal.segments',
      'org.graylog2.journal.entries-uncommitted',
    ];
    metricsStore.listen({
      nodeId: this.props.nodeId,
      metricNames: metricNames,
      callback: (update, hasError) => {
        if (hasError) {
          this.setState({hasError: hasError});
          return;
        }
        // update is [{nodeId, values: [{name, value: {metric}}]} ...]
        // metric can be various different things, depending on metric {type: "GAUGE"|"COUNTER"|"METER"|"TIMER"}

        const newState = {
          initialized: true,
          hasError: hasError,
        };
        // we will only get one result, because we ask for only one node
        // get the base name from the metric name, and put the gauge metrics into our new state.
        update[0].values.forEach((namedMetric) => {
          const baseName = namedMetric.name.replace('org.graylog2.journal.', '').replace('.1-sec-rate', '');
          const camelCase = this.camelCase(baseName);
          newState[camelCase] = namedMetric.metric.value;
        });
        this.setState(newState);
      },
    });
  },
  camelCase(input) {
    return input.toLowerCase().replace(/-(.)/g, (match, group1) => group1.toUpperCase());
  },
  render() {
    return (
      <span>
        <strong>{numeral(this.state.entriesUncommitted).format('0,0')} unprocessed messages</strong> are currently in the journal, in {this.state.segments}
        segments. <strong>
        {numeral(this.state.append).format('0,0')} messages</strong> have been appended to, and <strong>
        {numeral(this.state.read).format('0,0')} messages</strong> have been read from the journal in the last second.
      </span>
    );
  },
});

export default JournalState;
