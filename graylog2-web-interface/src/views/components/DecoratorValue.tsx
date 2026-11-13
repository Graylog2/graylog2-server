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
 import { useContext } from 'react';

import type FieldType from 'views/logic/fieldtypes/FieldType';
import FormattedValue from 'views/components/FormattedValue';
import { ActionContext } from 'views/logic/ActionContext';
import type { Message } from 'views/components/messagelist/Types';
import stringify from 'util/stringify';

import type { ValueRendererProps } from './messagelist/decoration/ValueRenderer';

type DecoratedLink = {
  type: string;
  href: string;
  text?: string;
};

const isDecoratedLink = (value: unknown): value is DecoratedLink =>
  typeof value === 'object' && value !== null && 'href' in value && 'type' in value && !!value.href && !!value.type;

const plainRenderer = ({ value }: ValueRendererProps) => value;

// Highlight ranges of a message refer to the original field value. They only apply to a link label which is
// identical to it, not to a custom link text or a templated URL.
const isOriginalFieldValue = (label: string, field: string, message: Message | undefined) => {
  const originalValue = message?.decoration_stats?.changed_fields?.[field]?.before;

  return originalValue !== undefined && stringify(originalValue) === label;
};

type Props = {
  field: string;
  value: any;
  truncate: boolean;
  render: React.ComponentType<ValueRendererProps>;
  type: FieldType;
};

const DecoratorValue = ({ field, value, truncate, render, type }: Props) => {
  const { message } = useContext(ActionContext);

  if (!isDecoratedLink(value)) {
    return <FormattedValue field={field} value={value} truncate={truncate} render={render} type={type} />;
  }

  const label = value.text ?? value.href;
  const labelRenderer = isOriginalFieldValue(label, field, message) ? render : plainRenderer;

  return (
    <a href={value.href} target="_blank" rel="noreferrer" title={value.text ? value.href : undefined}>
      <FormattedValue field={field} value={label} truncate={truncate} render={labelRenderer} type={type} />
    </a>
  );
};

export default DecoratorValue;
