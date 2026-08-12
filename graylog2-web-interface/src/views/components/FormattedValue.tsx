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
import trim from 'lodash/trim';
import trunc from 'lodash/truncate';

import type FieldType from 'views/logic/fieldtypes/FieldType';
import type { ValueRenderer } from 'views/components/messagelist/decoration/ValueRenderer';
import stringify from 'util/stringify';
import EmptyValue from 'views/components/EmptyValue';

type Props = {
  field: string;
  value: any;
  truncate: boolean;
  render: ValueRenderer;
  type: FieldType;
};
const FormattedValue = ({ field, value, truncate, render, type }: Props) => {
  const stringified = stringify(value);
  const Component = render;

  return trim(stringified) === '' ? (
    <EmptyValue />
  ) : (
    <Component field={field} value={truncate ? trunc(stringified) : stringified} type={type} />
  );
};

export default FormattedValue;
