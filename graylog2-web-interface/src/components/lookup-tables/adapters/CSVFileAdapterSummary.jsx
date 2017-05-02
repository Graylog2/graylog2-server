import React from 'react';

const CSVFileAdapterSummary = React.createClass({
  propTypes: {
    dataAdapter: React.PropTypes.object.isRequired,
  },

  render() {
    const config = this.props.dataAdapter.config;
    return (<dl>
      <dt>File path</dt>
      <dd>{config.path}</dd>
      <dt>Separator</dt>
      <dd><code>{config.separator}</code></dd>
      <dt>Quote character</dt>
      <dd><code>{config.quotechar}</code></dd>
      <dt>Key column</dt>
      <dd>{config.key_column}</dd>
      <dt>Value column</dt>
      <dd>{config.value_column}</dd>
      <dt>Check interval</dt>
      <dd>{config.check_interval} seconds</dd>
    </dl>);
  },
});


export default CSVFileAdapterSummary;
