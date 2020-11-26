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
import { isString, trim, truncate as trunc } from 'lodash';

import UserTimezoneTimestamp from 'views/components/common/UserTimezoneTimestamp';
import FieldType from 'views/logic/fieldtypes/FieldType';

import EmptyValue from './EmptyValue';
import CustomPropTypes from './CustomPropTypes';
import type { ValueRendererProps } from './messagelist/decoration/ValueRenderer';
import DecoratorValue from './DecoratorValue';

const _formatValue = (field, value, truncate, render, type) => {
  const stringified = isString(value) ? value : JSON.stringify(value);
  const Component = render;

  return trim(stringified) === ''
    ? <EmptyValue />
    : <Component field={field} value={(truncate ? trunc(stringified) : stringified)} type={type} />;
};

type Props = {
  field: string,
  value?: any,
  type: FieldType,
  truncate?: boolean,
  render?: React.ComponentType<ValueRendererProps>,
};

const defaultComponent = ({ value }: ValueRendererProps) => value;

const TypeSpecificValue = ({ field, value, render = defaultComponent, type = FieldType.Unknown, truncate = false }: Props) => {
  if (value === undefined) {
    return null;
  }

  if (type.isDecorated()) {
    return <DecoratorValue value={value} field={field} render={render} type={type} truncate={truncate} />;
  }

  switch (type.type) {
    case 'date': return <UserTimezoneTimestamp dateTime={value} />;
    case 'boolean': return <>{String(value)}</>;
    default: return _formatValue(field, value, truncate, render, type);
  }
};

TypeSpecificValue.propTypes = {
  truncate: PropTypes.bool,
  type: CustomPropTypes.FieldType,
  value: PropTypes.any,
};

TypeSpecificValue.defaultProps = {
  render: defaultComponent,
  value: undefined,
};

export default TypeSpecificValue;
