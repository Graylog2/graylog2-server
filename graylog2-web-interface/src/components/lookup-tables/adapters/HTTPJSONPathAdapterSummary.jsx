import React from 'react';
import PropTypes from 'prop-types';
import { KeyValueTable } from 'components/common';

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
    <dd><KeyValueTable pairs={config.headers || {}} /></dd>
  </dl>);
};

HTTPJSONPathAdapterSummary.propTypes = {
  dataAdapter: PropTypes.object.isRequired,
};

export default HTTPJSONPathAdapterSummary;
