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

import { Checkbox } from 'components/graylog';

import HeatmapVisualizationConfig, { COLORSCALES } from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';

import Select from '../Select';

type Props = {
  onChange: (config: HeatmapVisualizationConfig) => void,
  config: HeatmapVisualizationConfig,
};

const _makeOption = (value) => ({ label: value, value });
const colorScalesOptions = COLORSCALES.map(_makeOption);

const HeatmapVisualizationConfiguration = ({ config = HeatmapVisualizationConfig.empty(), onChange }: Props) => {
  const _onColorScaleChange = useCallback(({ value }) => onChange(config.toBuilder().colorScale(value).build()), [config, onChange]);
  const _onReverseScaleChange = useCallback((e) => onChange(config.toBuilder().reverseScale(e.target.checked).build()), [config, onChange]);

  return (
    <>
      <span>Color Scheme</span>
      <Select placeholder="Select Color Scheme"
              onChange={_onColorScaleChange}
              options={colorScalesOptions}
              value={_makeOption(config.colorScale)} />
      <span>Reverse Scale</span>
      <Checkbox onChange={_onReverseScaleChange}
                checked={config.reverseScale} />
    </>
  );
};

export default HeatmapVisualizationConfiguration;
