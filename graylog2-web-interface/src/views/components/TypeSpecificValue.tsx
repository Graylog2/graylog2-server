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
import isString from 'lodash/isString';
import trim from 'lodash/trim';
import trunc from 'lodash/truncate';

import Timestamp from 'components/common/Timestamp';
import FieldType from 'views/logic/fieldtypes/FieldType';
import InputField from 'views/components/fieldtypes/InputField';
import NodeField from 'views/components/fieldtypes/NodeField';
import StreamsField from 'views/components/fieldtypes/StreamsField';
import PercentageField from 'views/components/fieldtypes/PercentageField';
import { getPrettifiedValue } from 'views/components/visualizations/utils/unitConverters';
import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import { UNIT_FEATURE_FLAG } from 'views/components/visualizations/Constants';
import useFeature from 'hooks/useFeature';
import { MISSING_BUCKET_NAME } from 'views/Constants';
import formatValueWithUnitLabel from 'views/components/visualizations/utils/formatValueWithUnitLabel';

import EmptyValue from './EmptyValue';
import type { ValueRendererProps, ValueRenderer } from './messagelist/decoration/ValueRenderer';
import DecoratorValue from './DecoratorValue';

const defaultComponent = ({ value }: ValueRendererProps) => value;

const _formatValue = (field: string, value: any, truncate: boolean, render: ValueRenderer, type: FieldType) => {
  const stringified = isString(value) ? value : JSON.stringify(value);
  const Component: ValueRenderer = render;

  return trim(stringified) === ''
    ? <EmptyValue />
    : <Component field={field} value={(truncate ? trunc(stringified) : stringified)} type={type} />;
};

type TypeSpecificValueProps = {
  field: string,
  value?: any,
  type?: FieldType
  truncate?: boolean,
  render?: React.ComponentType<ValueRendererProps>,
  unit?: FieldUnit,
};

const ValueWithUnitRenderer = ({ value, unit }: { value: number, unit: FieldUnit}) => {
  const prettified = getPrettifiedValue(value, { abbrev: unit.abbrev, unitType: unit.unitType });

  return <span title={value.toString()}>{formatValueWithUnitLabel(prettified?.value, prettified.unit.abbrev)}</span>;
};

const FormattedValue = ({ field, value, truncate = false, render = defaultComponent, unit, type }: TypeSpecificValueProps) => {
  const unitFeatureEnabled = useFeature(UNIT_FEATURE_FLAG);
  const shouldRenderValueWithUnit = unitFeatureEnabled && unit?.isDefined && value && value !== MISSING_BUCKET_NAME && unitFeatureEnabled;
  if (shouldRenderValueWithUnit) return <ValueWithUnitRenderer value={value} unit={unit} />;

  return _formatValue(field, value, truncate, render, type);
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
    default: return <FormattedValue field={field} value={value} truncate={truncate} unit={unit} render={render} type={type} />;
  }
};

export default TypeSpecificValue;
