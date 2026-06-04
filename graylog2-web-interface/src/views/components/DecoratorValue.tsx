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

import type FieldType from 'views/logic/fieldtypes/FieldType';
import FormattedValue from 'views/components/FormattedValue';

import type { ValueRendererProps } from './messagelist/decoration/ValueRenderer';

type Props = {
  field: string;
  value: any;
  truncate: boolean;
  render: React.ComponentType<ValueRendererProps>;
  type: FieldType;
};

const DecoratorValue = ({ field, value, truncate, render, type }: Props) =>
  value && value.href && value.type ? (
    <a href={value.href} target="_blank" rel="noreferrer">
      <FormattedValue field={field} value={value.href} truncate={truncate} render={render} type={type} />
    </a>
  ) : (
    <FormattedValue field={field} value={value} truncate={truncate} render={render} type={type} />
  );

export default DecoratorValue;
