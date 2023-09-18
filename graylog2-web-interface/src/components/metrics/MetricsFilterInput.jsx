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
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';

import { FormControl } from 'components/bootstrap';

const MetricsFilterInput = ({ filter, onChange }) => {
  const handleChange = useCallback((event) => onChange(event.target.value), []);

  return (
    <FormControl type="text"
                 className="metrics-filter"
                 bsSize="large"
                 placeholder="Type a metric name to filter&hellip;"
                 value={filter}
                 onChange={handleChange} />
  );
};

MetricsFilterInput.propTypes = {
  filter: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default MetricsFilterInput;
