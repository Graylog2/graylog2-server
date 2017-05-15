import React from 'react';

const HTTPJSONPathAdapterSummary = React.createClass({
  propTypes: {
    dataAdapter: React.PropTypes.object.isRequired,
  },

  render() {
    const config = this.props.dataAdapter.config;
    return (<dl>
      <dt>Lookup URL</dt>
      <dd>{config.url}</dd>
      <dt>Single value JSONPath</dt>
      <dd><code>{config.single_value_jsonpath}</code></dd>
      <dt>Multi value JSONPath</dt>
      <dd><code>{config.multi_value_jsonpath}</code></dd>
      <dt>HTTP User-Agent</dt>
      <dd>{config.user_agent}</dd>
    </dl>);
  },
});


export default HTTPJSONPathAdapterSummary;
