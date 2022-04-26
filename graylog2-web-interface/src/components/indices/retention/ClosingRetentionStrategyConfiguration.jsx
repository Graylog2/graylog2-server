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
import * as React from 'react';
import { useState } from 'react';

import { getValueFromInput } from 'util/FormsUtils';
import { Input } from 'components/bootstrap';

const ClosingRetentionStrategyConfiguration = ({ config, updateConfig }) => {
  const { max_number_of_indices } = config;
  const [maxNumberOfIndices, setMaxNumberOfIndices] = useState(max_number_of_indices);

  const _onInputUpdate = (field) => {
    return (e) => {
      const update = {};
      const value = getValueFromInput(e.target);
      update[field] = value;

      setMaxNumberOfIndices(value);
      updateConfig(update);
    };
  };

  return (
    <div>
      <Input type="number"
             id="max-number-of-indices"
             label="Max number of indices"
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9"
             onChange={_onInputUpdate('max_number_of_indices')}
             value={maxNumberOfIndices}
             help={<span>Maximum number of indices to keep before <strong>closing</strong> the oldest ones</span>}
             required />
    </div>
  );
};

ClosingRetentionStrategyConfiguration.propTypes = {
  config: PropTypes.object.isRequired,
  updateConfig: PropTypes.func.isRequired,
};

export default ClosingRetentionStrategyConfiguration;
