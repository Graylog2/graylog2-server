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

import NumberUtils from 'util/NumberUtils';

const SizeBasedRotationStrategySummary = ({ config }) => {
  const { max_size: maxSize } = config;

  return (
    <div>
      <dl>
        <dt>Index rotation strategy:</dt>
        <dd>Index Size</dd>
        <dt>Max index size:</dt>
        <dd>{maxSize} bytes ({NumberUtils.formatBytes(maxSize)})</dd>
      </dl>
    </div>
  );
};

SizeBasedRotationStrategySummary.propTypes = {
  config: PropTypes.object.isRequired,
};

export default SizeBasedRotationStrategySummary;
