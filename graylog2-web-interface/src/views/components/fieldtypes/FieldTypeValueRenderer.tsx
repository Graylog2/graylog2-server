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
  { type: 'input', render: (_flied, value) => <InputField value={String(value)} /> },
  { type: 'node', render: (_flied, value) => <NodeField value={String(value)} /> },
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
