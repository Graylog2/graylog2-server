/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

class CSVFileAdapterSummary extends React.Component {
  static propTypes = {
    dataAdapter: PropTypes.object.isRequired,
  };

  render() {
    const { config } = this.props.dataAdapter;

    return (
      <dl>
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
      </dl>
    );
  }
}

export default CSVFileAdapterSummary;
