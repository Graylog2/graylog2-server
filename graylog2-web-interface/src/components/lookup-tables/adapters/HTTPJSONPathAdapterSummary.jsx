import React from 'react';
import PropTypes from 'prop-types';

const HTTPJSONPathAdapterSummary = ({ dataAdapter }) => {
  const { config } = dataAdapter;
  return (<dl>
    <dt>Lookup URL</dt>
    <dd>{config.url}</dd>
    <dt>Single value JSONPath</dt>
    <dd><code>{config.single_value_jsonpath}</code></dd>
    <dt>Multi value JSONPath</dt>
    <dd><code>{config.multi_value_jsonpath}</code></dd>
    <dt>HTTP User-Agent</dt>
    <dd>{config.user_agent}</dd>
    <dt>HTTP Headers</dt>
    <dd><pre>{config.headers ? Object.keys(config.headers).map(key => <p key={key} style={{ 'margin-bottom': 0 }}>{key}: {config.headers[key]}</p>) : <i>Empty</i>}</pre></dd>
  </dl>);
};

HTTPJSONPathAdapterSummary.propTypes = {
  dataAdapter: PropTypes.object.isRequired,
};

export default HTTPJSONPathAdapterSummary;
