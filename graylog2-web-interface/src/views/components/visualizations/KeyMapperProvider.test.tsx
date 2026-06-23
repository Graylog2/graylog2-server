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
import { render, screen } from 'wrappedTestingLibrary';
import { PluginStore } from 'graylog-web-plugin/plugin';
import * as Immutable from 'immutable';

import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import KeyMapperProvider from './KeyMapperProvider';
import KeyMapperContext from './KeyMapperContext';

const testPlugin = {
  exports: {
    visualizationKeyMappers: [
      {
        type: 'streams',
        useKeyMapper: () => (key: string) => (key === 'stream-1' ? 'All messages' : key),
      },
    ],
  },
};

const config = {
  rowPivots: [{ fields: ['streams'] }],
  columnPivots: [],
} as unknown as AggregationWidgetConfig;

const fields = Immutable.List([new FieldTypeMapping('streams', FieldType.create('streams', []))]);

const data = {
  chart: [{ source: 'leaf', key: ['stream-1'], values: [] }],
} as any;

const Consumer = ({ k }: { k: string }) => {
  const mapKeys = useContext(KeyMapperContext);

  return <span>{mapKeys(k, 'streams')}</span>;
};

describe('KeyMapperProvider', () => {
  beforeAll(() => PluginStore.register(testPlugin));
  afterAll(() => PluginStore.unregister(testPlugin));

  it('provides a mapper that resolves keys via the registered binding for the field type', () => {
    render(
      <KeyMapperProvider data={data} config={config} fields={fields}>
        <Consumer k="stream-1" />
      </KeyMapperProvider>,
    );

    expect(screen.getByText('All messages')).toBeInTheDocument();
  });

  it('falls back to the raw key for unknown values', () => {
    render(
      <KeyMapperProvider data={data} config={config} fields={fields}>
        <Consumer k="stream-unknown" />
      </KeyMapperProvider>,
    );

    expect(screen.getByText('stream-unknown')).toBeInTheDocument();
  });
});
