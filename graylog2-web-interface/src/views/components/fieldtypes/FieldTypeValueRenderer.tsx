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
import type { PluginExports } from 'graylog-web-plugin/plugin';

import Timestamp from 'components/common/Timestamp';

import InputField from './InputField';
import NodeField from './NodeField';
import StreamsField from './StreamsField';
import PercentageField from './PercentageField';
import EventDefinition from './EventDefinition';

const FieldTypeValueRenderer: PluginExports['fieldTypeValueRenderer'] = [
  {
    type: 'date',
    render: (value: string, field, render) => (
      <Timestamp dateTime={value} render={render} field={field} format="complete" />
    ),
  },
  {
    type: 'boolean',
    render: (value, field, Component) => <Component value={String(value)} field={field} />,
  },
  { type: 'input', render: (value) => <InputField value={String(value)} /> },
  { type: 'node', render: (value) => <NodeField value={String(value)} /> },
  {
    type: 'streams',
    render: (value: string | Array<string>) => <StreamsField value={value} />,
  },
  { type: 'percentage', render: (value: number) => <PercentageField value={value} /> },
  {
    type: 'event-definition-id',
    render: (value: string) => <EventDefinition value={value} />,
  },
];

export default FieldTypeValueRenderer;
