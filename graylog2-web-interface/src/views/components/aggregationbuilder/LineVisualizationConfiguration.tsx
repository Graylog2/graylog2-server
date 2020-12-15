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
import React, { useCallback } from 'react';
import { capitalize } from 'lodash';

import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';

import Select from '../Select';

type Props = {
  onChange: (config: LineVisualizationConfig) => void,
  config: LineVisualizationConfig,
};

const _makeOption = (value) => ({ label: capitalize(value), value });
const interpolationOptions = ['linear', 'step-after', 'spline'].map(_makeOption);

const LineVisualizationConfiguration = ({ config = LineVisualizationConfig.empty(), onChange }: Props) => {
  const _onChange = useCallback(({ value }) => onChange(config.toBuilder().interpolation(value).build()), [config, onChange]);

  return (
    <>
      <span>Interpolation:</span>
      <Select placeholder="Select Interpolation Mode"
              onChange={_onChange}
              options={interpolationOptions}
              value={_makeOption(config.interpolation)} />
    </>
  );
};

export default LineVisualizationConfiguration;
