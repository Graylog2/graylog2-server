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
