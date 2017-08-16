import React from 'react';

const DSVHTTPAdapterSummary = ({ dataAdapter }) => {
  const { config } = dataAdapter;

  return (<dl>
    <dt>File URL</dt>
    <dd>{config.url}</dd>
    <dt>Separator</dt>
    <dd><code>{config.separator}</code></dd>
    <dt>Quote character</dt>
    <dd><code>{config.quotechar}</code></dd>
    <dt>Ignore lines starting with</dt>
    <dd><code>{config.ignorechar}</code></dd>
    <dt>Key column</dt>
    <dd>{config.key_column}</dd>
    <dt>Value column</dt>
    <dd>{config.value_column}</dd>
    <dt>Check interval</dt>
    <dd>{config.check_interval} seconds</dd>
    <dt>Case-insensitive lookup</dt>
    <dd>{config.case_insensitive_lookup ? 'yes' : 'no'}</dd>
  </dl>);
};

export default DSVHTTPAdapterSummary;
