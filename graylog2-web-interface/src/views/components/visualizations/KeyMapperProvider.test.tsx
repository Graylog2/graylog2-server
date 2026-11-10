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
import * as Immutable from 'immutable';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import { usePlugin } from 'views/test/testPlugins';

import KeyMapperProvider from './KeyMapperProvider';
import KeyMapperContext from './KeyMapperContext';

const testPlugin = {
  exports: {
    visualizationKeyMappers: [
      {
        type: 'streams',
        useKeyMapper: () => (key: string) => (key === 'stream-1' ? 'All messages' : key),
      },
      // Asset-style binding: only resolves keys it was actually handed by the provider, so a test
      // fails if collectKeysByType does not prefetch the right ids for a given pivot layout.
      {
        type: 'associated-assets',
        useKeyMapper: (keys: Array<string>) => (key: string) => (keys.includes(String(key)) ? `Asset ${key}` : key),
      },
    ],
  },
};

const config = AggregationWidgetConfig.builder()
  .rowPivots([Pivot.createValues(['streams'])])
  .columnPivots([])
  .build();

const fields = Immutable.List([
  new FieldTypeMapping('streams', FieldType.create('streams', [])),
  new FieldTypeMapping('a', FieldType.create('streams', [])),
  new FieldTypeMapping('b', FieldType.create('associated-assets', [])),
]);

const data = {
  chart: [{ source: 'leaf', key: ['stream-1'], values: [] }],
} as any;

const Consumer = ({ k, field = 'streams' }: { k: string; field?: string }) => {
  const mapKeys = useContext(KeyMapperContext);

  return <span>{mapKeys(k, field)}</span>;
};

describe('KeyMapperProvider', () => {
  usePlugin(testPlugin);

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

  it('prefetches and resolves keys from the second of two row pivots (sankey/network layout)', () => {
    const twoRowPivots = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .columnPivots([])
      .build();
    const twoRowPivotData = {
      chart: [
        {
          source: 'leaf',
          key: ['a1', 'b1'],
          values: [{ source: 'row-leaf', key: ['count()'], value: 5, rollup: true }],
        },
        {
          source: 'leaf',
          key: ['a1', 'b2'],
          values: [{ source: 'row-leaf', key: ['count()'], value: 3, rollup: true }],
        },
      ],
    } as any;

    render(
      <KeyMapperProvider data={twoRowPivotData} config={twoRowPivots} fields={fields}>
        <Consumer k="b1" field="b" />
      </KeyMapperProvider>,
    );

    expect(screen.getByText('Asset b1')).toBeInTheDocument();
  });

  it('prefetches and resolves keys from a column pivot (one row + one column layout)', () => {
    const rowAndColumn = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a'])])
      .columnPivots([Pivot.createValues(['b'])])
      .build();
    const rowAndColumnData = {
      chart: [
        {
          source: 'leaf',
          key: ['a1'],
          values: [{ source: 'col-leaf', key: ['b1', 'count()'], value: 5, rollup: false }],
        },
      ],
    } as any;

    render(
      <KeyMapperProvider data={rowAndColumnData} config={rowAndColumn} fields={fields}>
        <Consumer k="b1" field="b" />
      </KeyMapperProvider>,
    );

    expect(screen.getByText('Asset b1')).toBeInTheDocument();
  });
});
