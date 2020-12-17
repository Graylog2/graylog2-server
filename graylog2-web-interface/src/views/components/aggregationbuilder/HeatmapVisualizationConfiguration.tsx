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
import styled from 'styled-components';

import { Checkbox } from 'components/graylog';
import { Input } from 'components/bootstrap';
import HeatmapVisualizationConfig, { COLORSCALES } from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';

import Select from '../Select';

const StyledInput = styled(Input)`
  > label {
    font-weight: normal;
  }
`;

type Props = {
  onChange: (config: HeatmapVisualizationConfig) => void,
  config: HeatmapVisualizationConfig,
};

const _makeOption = (value) => ({ label: value, value });
const colorScalesOptions = COLORSCALES.map(_makeOption);

const _validateConfig = (config: HeatmapVisualizationConfig, setErrors) => {

}

const HeatmapVisualizationConfiguration = ({ config = HeatmapVisualizationConfig.empty(), onChange }: Props) => {
  const _onColorScaleChange = useCallback(({ value }) => onChange(config.toBuilder().colorScale(value).build()), [config, onChange]);
  const _onReverseScaleChange = useCallback((e) => onChange(config.toBuilder().reverseScale(e.target.checked).build()), [config, onChange]);
  const _onDefaultValueChange = useCallback((e) => onChange(config.toBuilder().defaultValue(e.target.value).build()), [config, onChange]);
  const _onAutoScaleChange = useCallback((e) => onChange(config.toBuilder().autoScale(e.target.checked).build()), [config, onChange]);
  const _onZminChange = useCallback((e) => onChange(config.toBuilder().zMin(e.target.value).build()), [config, onChange]);
  const _onZmidChange = useCallback((e) => onChange(config.toBuilder().zMid(e.target.value).build()), [config, onChange]);
  const _onZmaxChange = useCallback((e) => onChange(config.toBuilder().zMax(e.target.value).build()), [config, onChange]);

  return (
    <>
      <span>Color Scheme</span>
      <Select placeholder="Select Color Scheme"
              onChange={_onColorScaleChange}
              options={colorScalesOptions}
              value={_makeOption(config.colorScale)} />
      <span>Default</span>
      <StyledInput type="number"
                   onChange={_onDefaultValueChange}
                   label="Default Value" />
      <span>Scale settings</span>
      <Checkbox onChange={_onReverseScaleChange}
                checked={config.reverseScale}>
        Reverse Scale
      </Checkbox>
      <Checkbox onChange={_onAutoScaleChange}
                checked={config.autoScale}>
        Auto Scale
      </Checkbox>
      <StyledInput type="number"
                   disabled={config.autoScale}
                   onChange={_onZminChange}
                   value={config.zMin}
                   label="Min" />
      <StyledInput type="number"
                   disabled={config.autoScale}
                   value={config.zMax}
                   onChange={_onZmaxChange}
                   label="Max" />
    </>
  );
};

export default HeatmapVisualizationConfiguration;
