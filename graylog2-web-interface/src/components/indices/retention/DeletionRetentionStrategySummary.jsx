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

import { TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY } from 'stores/indices/IndicesStore';

const DeletionRetentionStrategySummary = ({ config, rotationStrategyClass }) => (
  <div>
    <dl>
      <dt>Index retention strategy:</dt>
      <dd>Delete</dd>
      {rotationStrategyClass !== TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY && (
      <>
        <dt>Max number of indices:</dt>
        <dd>{config.max_number_of_indices}</dd>
      </>
      )}
    </dl>
  </div>
);

DeletionRetentionStrategySummary.propTypes = {
  config: PropTypes.object.isRequired,
  rotationStrategyClass: PropTypes.string,
};

DeletionRetentionStrategySummary.defaultProps = {
  rotationStrategyClass: undefined,
};

export default DeletionRetentionStrategySummary;
