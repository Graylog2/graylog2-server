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
import { trim } from 'lodash';
import { Field, useFormikContext } from 'formik';

import { Input } from 'components/bootstrap';
import { ColorPickerPopover, Select } from 'components/common';
import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import ColorPreview, { GradientColorPreview } from 'views/components/sidebar/highlighting/ColorPreview';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import { COLORSCALES } from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';
import { defaultCompare } from 'logic/DefaultCompare';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

type Props = {
  field: FieldTypeMapping,
};

type ColorPickerProps = {
  type: string,
};

export type StaticColorObject = {
  type: 'static',
  color: string,
};

export type GradientColorObject = {
  type: 'gradient',
  gradient: string,
  upper: number,
  lower: number,
};

export type ColorType = StaticColorObject['type'] | GradientColorObject['type'];

const StaticColorPicker = () => (
  <Field name="color.color">
    {({ field: { name, value, onChange }, meta }) => (
      <Input id={name}
             error={meta?.error}
             label="Color">
        <ColorPickerPopover id="formatting-rule-color"
                            placement="right"
                            color={value}
                            colors={DEFAULT_CUSTOM_HIGHLIGHT_RANGE.map((c) => [c])}
                            triggerNode={<ColorPreview color={StaticColor.create(value)} />}
                            onChange={(newColor, _, hidePopover) => {
                              hidePopover();
                              onChange({ target: { name, value: newColor } });
                            }} />
      </Input>
    )}
  </Field>
);

const OptionContainer = styled.div`
  display: flex;
`;
const createOption = (name) => ({ label: <OptionContainer><GradientColorPreview gradient={name} />{name}</OptionContainer>, value: name });

const GRADIENTS = [...COLORSCALES].sort(defaultCompare).map(createOption);

type ColorErrors = Record<string, string>;

const numericRegex = /^-?[0-9]+$/;

const validateColor = (values) => {
  const errors: ColorErrors = {};

  if (values?.type === 'gradient') {
    if (trim(values?.gradient) === '') {
      errors.gradient = 'Must be selected.';
    }

    if (trim(values?.lower) === '') {
      errors.lower = 'Must be present.';
    } else if (!numericRegex.test(values?.lower)) {
      errors.lower = 'Must be a number.';
    }

    if (trim(values?.upper) === '') {
      errors.upper = 'Must be present.';
    } else if (!numericRegex.test(values?.upper)) {
      errors.upper = 'Must be a number.';
    }

    if (values?.upper <= values?.lower) {
      errors.upper = 'Must be higher than lowest value.';
    }
  }

  return Object.keys(errors).length > 0 ? errors : undefined;
};

const GradientColorPicker = () => {
  return (
    <Field name="color" validate={validateColor}>
      {() => (
        <>
          <Field name="color.gradient">
            {({ field: { name, value, onChange }, meta }) => (
              <Input id={`${name}-name`}
                     error={meta?.error}
                     label="Gradient Name">
                <Select options={GRADIENTS}
                        inputProps={{ 'aria-label': 'Select gradient colors' }}
                        value={value}
                        onChange={(newGradient) => onChange({ target: { name, value: newGradient } })} />
              </Input>
            )}
          </Field>
          <Field name="color.lower">
            {({ field: { name, value, onChange }, meta }) => (
              <Input id={name}
                     aria-label="Specify lowest value"
                     label="Lowest Value"
                     type="number"
                     value={value}
                     error={meta?.error}
                     onChange={onChange}
                     help="The lowest value expected in the field/series."
                     required />
            )}
          </Field>
          <Field name="color.upper">
            {({ field: { name, value, onChange }, meta }) => (
              <Input id={name}
                     aria-label="Specify highest value"
                     label="Highest Value"
                     type="number"
                     value={value}
                     error={meta?.error}
                     onChange={onChange}
                     help="The highest value expected in the field/series."
                     required />
            )}
          </Field>
        </>
      )}
    </Field>
  );
};

const ColorForm = ({ type }: ColorPickerProps) => {
  switch (type) {
    case 'static': return <StaticColorPicker />;
    case 'gradient': return <GradientColorPicker />;
    default: return null;
  }
};

const Container = styled.div`
  margin-left: 10px;
`;

const randomColor = () => DEFAULT_CUSTOM_HIGHLIGHT_RANGE[
  Math.floor(Math.random() * DEFAULT_CUSTOM_HIGHLIGHT_RANGE.length)
];

export const createNewColor = (newType: ColorType): StaticColorObject | GradientColorObject => {
  if (newType === 'static') {
    return {
      type: 'static',
      color: randomColor(),
    };
  }

  if (newType === 'gradient') {
    return {
      type: 'gradient',
      gradient: 'Viridis',
      upper: 0,
      lower: 0,
    };
  }

  throw new Error(`Invalid color type: ${newType}`);
};

export const validateColoringType = (value, fieldIsNumeric) => {
  if (!value || value === '') {
    return 'Coloring is required';
  }

  if (!fieldIsNumeric && value === 'gradient') {
    return 'A gradient can only be defined for numeric fields.';
  }

  return undefined;
};

const HighlightingColorForm = ({ field }: Props) => {
  const isNumeric = field?.type?.isNumeric() ?? false;

  const { setFieldValue } = useFormikContext();

  const onChangeType = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const { target: { value: newType } } = e;
    setFieldValue('color', createNewColor(newType as ColorType));
  }, [setFieldValue]);

  return (
    <Field name="color.type" validate={(value) => validateColoringType(value, isNumeric)}>
      {({ field: { name, value }, meta }) => (
        <>
          <Input id={`${name}-coloring`}
                 label="Coloring"
                 error={meta?.error}>
            <Container>
              <Input checked={value === 'static'}
                     formGroupClassName=""
                     id={name}
                     label="Static Color"
                     onChange={onChangeType}
                     type="radio"
                     value="static" />
              <Input checked={value === 'gradient'}
                     formGroupClassName=""
                     id={name}
                     disabled={!isNumeric}
                     label="Gradient"
                     onChange={onChangeType}
                     type="radio"
                     value="gradient" />
            </Container>
          </Input>
          {value && <ColorForm type={value} />}
        </>
      )}
    </Field>
  );
};

export default HighlightingColorForm;
