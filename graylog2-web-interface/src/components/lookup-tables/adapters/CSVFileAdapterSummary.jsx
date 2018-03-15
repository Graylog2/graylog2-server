import PropTypes from 'prop-types';
import React from 'react';

class CSVFileAdapterSummary extends React.Component {
  static propTypes = {
    dataAdapter: PropTypes.object.isRequired,
  };

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
      <dt>Case-insensitive lookup</dt>
      <dd>{config.case_insensitive_lookup ? 'yes' : 'no'}</dd>
    </dl>);
  }
}


export default CSVFileAdapterSummary;
