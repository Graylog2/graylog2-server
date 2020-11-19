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
// @flow strict
import * as React from 'react';
import { isString, trim, truncate as trunc } from 'lodash';

import FieldType from 'views/logic/fieldtypes/FieldType';

import EmptyValue from './EmptyValue';
import type { ValueRendererProps } from './messagelist/decoration/ValueRenderer';

const _formatValue = (field, value, truncate, render, type) => {
  const stringified = isString(value) ? value : JSON.stringify(value);
  const Component = render;

  return trim(stringified) === ''
    ? <EmptyValue />
    : <Component field={field} value={(truncate ? trunc(stringified) : stringified)} type={type} />;
};

type Props = {
  field: string,
  value: any,
  truncate: boolean,
  render: React.ComponentType<ValueRendererProps>,
  type: FieldType,
};

const DecoratorValue = ({ field, value, truncate, render, type }: Props) => {
  if (value && value.href && value.type) {
    const formattedValue = _formatValue(field, value.href, truncate, render, type);

    return <a href={value.href}>{formattedValue}</a>;
  }

  return _formatValue(field, value, truncate, render, type);
};

export default DecoratorValue;
