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

import HeatmapVisualizationConfig, { COLORSCALES } from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';

import Select from '../Select';

type Props = {
  onChange: (config: HeatmapVisualizationConfig) => void,
  config: HeatmapVisualizationConfig,
};

const _makeOption = (value) => ({ label: value, value });
const colorScalesOptions = COLORSCALES.map(_makeOption);

const HeatmapVisualizationConfiguration = ({ config = HeatmapVisualizationConfig.empty(), onChange }: Props) => {
  const _onChange = useCallback(({ value }) => onChange(config.toBuilder().colorScale(value).build()), [config, onChange]);

  return (
    <>
      <span>Color Scheme</span>
      <Select placeholder="Select Color Scheme"
              onChange={_onChange}
              options={colorScalesOptions}
              value={_makeOption(config.colorScale)} />
    </>
  );
};

export default HeatmapVisualizationConfiguration;
