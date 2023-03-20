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
import React from 'react';
import PropTypes from 'prop-types';

const WhoisAdapterSummary = ({ dataAdapter }) => {
  const { config } = dataAdapter;

  return (<dl>
    <dt>Connect timeout</dt>
    <dd>{config.connect_timeout} ms</dd>
    <dt>Read timeout</dt>
    <dd>{config.read_timeout} ms</dd>
  </dl>);
};

WhoisAdapterSummary.propTypes = {
  dataAdapter: PropTypes.shape({
    config: PropTypes.shape({
      connect_timeout: PropTypes.number.isRequired,
      read_timeout: PropTypes.number.isRequired,
    }),
  }).isRequired,
};

export default WhoisAdapterSummary;
