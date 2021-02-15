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
import styled from 'styled-components';

import { Input } from 'components/bootstrap';
import { ColorPickerPopover, Select } from 'components/common';
import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import ColorPreview, { GradientColorPreview } from 'views/components/sidebar/highlighting/ColorPreview';
import HighlightingColor, {
  GradientColor,
  StaticColor,
} from 'views/logic/views/formatting/highlighting/HighlightingColor';
import { COLORSCALES } from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';
import { defaultCompare } from 'views/logic/DefaultCompare';

type ChangeEvent = {
  target: {
    name: string,
    value: HighlightingColor,
  },
};

type Props = {
  name: string,
  value: HighlightingColor,
  onChange: (e: ChangeEvent) => void,
};

const StaticColorPicker = ({ name, value, onChange }: { name: string, value: StaticColor, onChange: (newColor: HighlightingColor) => void}) => (
  <Input id={name}
         label="Color">
    <ColorPickerPopover id="formatting-rule-color"
                        placement="right"
                        color={value.color}
                        colors={DEFAULT_CUSTOM_HIGHLIGHT_RANGE.map((c) => [c])}
                        triggerNode={<ColorPreview color={value} />}
                        onChange={(newColor, _, hidePopover) => {
                          hidePopover();
                          onChange(StaticColor.create(newColor));
                        }} />
  </Input>
);

const OptionContainer = styled.div`
  display: flex
`;
const createOption = (name) => ({ label: <OptionContainer><GradientColorPreview gradient={name} />{name}</OptionContainer>, value: name });

const GRADIENTS = COLORSCALES.sort(defaultCompare).map(createOption);

const GradientColorPicker = ({ name, value, onChange }: { name: string, value: GradientColor, onChange: (newColor: GradientColor) => void}) => {
  const _onChangeGradient = useCallback((newGradient) => onChange(value.withGradient(newGradient)), [onChange, value]);
  const _onChangeLower = useCallback(({ target: { value: newLower } }) => onChange(value.withLower(newLower)), [onChange, value]);
  const _onChangeUpper = useCallback(({ target: { value: newUpper } }) => onChange(value.withUpper(newUpper)), [onChange, value]);

  return (
    <>
      <Input id={name}
             label="Gradient">
        <Select options={GRADIENTS}
                value={value.gradient}
                onChange={_onChangeGradient} />
      </Input>
      <Input id={name}
             label="Lowest Value"
             type="number"
             value={value.lower}
             onChange={_onChangeLower}
             help="The lowest value expected in the field/series."
             required />
      <Input id={name}
             label="Highest Value"
             type="number"
             value={value.upper}
             onChange={_onChangeUpper}
             help="The highest value expected in the field/series."
             required />
    </>
  );
};

const createNewColor = (type: 'static' | 'gradient') => {
  switch (type) {
    case 'gradient': return GradientColor.create('Viridis', 0, 0);
    case 'static':
    default:
      return StaticColor.create('#dddddd');
  }
};

const ColorForm = ({ color, name, onChange }: { color: HighlightingColor, name: string, onChange: (newColor: HighlightingColor) => void }) => {
  switch (color.type) {
    case 'static': return <StaticColorPicker value={color as StaticColor} name={name} onChange={onChange} />;
    case 'gradient': return <GradientColorPicker value={color as GradientColor} name={name} onChange={onChange} />;
    default: return null;
  }
};

const HighlightingColorForm = ({ name, value, onChange }: Props) => {
  const onChangeType = useCallback(({ target: { value: newValue } }) => onChange({ target: { name, value: createNewColor(newValue) } }), [name, onChange]);
  const _onChange = useCallback((newColor: HighlightingColor) => onChange({ target: { name, value: newColor } }), [name, onChange]);

  return (
    <>
      <Input id={name}
             label="Color">
        <Input checked={value?.type === 'static'}
               formGroupClassName=""
               id={name}
               label="Static Color"
               onChange={onChangeType}
               type="radio"
               value="static" />
        <Input checked={value?.type === 'gradient'}
               formGroupClassName=""
               id={name}
               label="Gradient"
               onChange={onChangeType}
               type="radio"
               value="gradient" />
      </Input>
      <ColorForm color={value} onChange={_onChange} name={name} />
    </>
  );
};

export default HighlightingColorForm;
