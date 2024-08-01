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
import PropTypes from 'prop-types';
import isString from 'lodash/isString';
import trim from 'lodash/trim';
import trunc from 'lodash/truncate';

import Timestamp from 'components/common/Timestamp';
import FieldType from 'views/logic/fieldtypes/FieldType';
import InputField from 'views/components/fieldtypes/InputField';
import NodeField from 'views/components/fieldtypes/NodeField';
import StreamsField from 'views/components/fieldtypes/StreamsField';
import PercentageField from 'views/components/fieldtypes/PercentageField';
import { getPrettifiedValue } from 'views/components/visualizations/utils/unitConvertors';
import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import { DECIMAL_PLACES } from 'views/components/visualizations/Constants';

import EmptyValue from './EmptyValue';
import CustomPropTypes from './CustomPropTypes';
import type { ValueRendererProps } from './messagelist/decoration/ValueRenderer';
import DecoratorValue from './DecoratorValue';

const defaultComponent = ({ value }: ValueRendererProps) => value;

const _formatValue = (field: string, value: any, truncate: boolean, render: React.ComponentType<ValueRendererProps>) => {
  const stringified = isString(value) ? value : JSON.stringify(value);
  const Component = render;

  return trim(stringified) === ''
    ? <EmptyValue />
    : <Component field={field} value={(truncate ? trunc(stringified) : stringified)} />;
};

type TypeSpecificValueProps = {
  field: string,
  value?: any,
  type: FieldType,
  truncate?: boolean,
  render?: React.ComponentType<ValueRendererProps>,
  unit?: FieldUnit,
};

type FormattedValueProps = Omit<TypeSpecificValueProps, 'type'>;

const ValueWithUnitRenderer = ({ value, unit }: { value: number, unit: FieldUnit}) => {
  const prettified = getPrettifiedValue(value, { abbrev: unit.abbrev, unitType: unit.unitType });

  return <span title={value.toString()}>{`${Number(prettified?.value).toFixed(DECIMAL_PLACES)} ${prettified.unit.abbrev}`}</span>;
};

const FormattedValue = ({ field, value, truncate, render, unit }: FormattedValueProps) => {
  if (unit?.isDefined && value) return <ValueWithUnitRenderer value={value} unit={unit} />;

  return _formatValue(field, value, truncate, render);
};

FormattedValue.defaultProps = {
  value: undefined,
  truncate: false,
  render: defaultComponent,
  unit: undefined,
};

const TypeSpecificValue = ({ field, value, render = defaultComponent, type = FieldType.Unknown, truncate = false, unit }: TypeSpecificValueProps) => {
  const Component = render;

  if (value === undefined) {
    return null;
  }

  if (type.isDecorated()) {
    return <DecoratorValue value={value} field={field} render={render} type={type} truncate={truncate} />;
  }

  switch (type.type) {
    case 'date': return <Timestamp dateTime={value} render={render} field={field} format="complete" />;
    case 'boolean': return <Component value={String(value)} field={field} />;
    case 'input': return <InputField value={String(value)} />;
    case 'node': return <NodeField value={String(value)} />;
    case 'streams': return <StreamsField value={value} />;
    case 'percentage': return <PercentageField value={value} />;
    default: return <FormattedValue field={field} value={value} truncate={truncate} unit={unit} render={render} />;
  }
};

TypeSpecificValue.propTypes = {
  truncate: PropTypes.bool,
  type: CustomPropTypes.FieldType,
  value: PropTypes.any,
};

TypeSpecificValue.defaultProps = {
  truncate: false,
  render: defaultComponent,
  type: undefined,
  value: undefined,
  unit: undefined,
};

export default TypeSpecificValue;
