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

const MessageCountRotationStrategyConfiguration = ({ config, updateConfig }) => {
  const { max_docs_per_index } = config;
  const [maxDocsPerIndex, setMaxDocsPerIndex] = useState(max_docs_per_index);

  const _onInputUpdate = (field) => {
    return (e) => {
      const update = {};
      const value = getValueFromInput(e.target);
      update[field] = value;

      setMaxDocsPerIndex(value);
      updateConfig(update);
    };
  };

  return (
    <div>
      <Input type="number"
             id="max-docs-per-index"
             label="Max documents per index"
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9"
             onChange={_onInputUpdate('max_docs_per_index')}
             value={maxDocsPerIndex}
             help="Maximum number of documents in an index before it gets rotated"
             required />
    </div>
  );
};

MessageCountRotationStrategyConfiguration.propTypes = {
  config: PropTypes.object.isRequired,
  updateConfig: PropTypes.func.isRequired,
};

export default MessageCountRotationStrategyConfiguration;
