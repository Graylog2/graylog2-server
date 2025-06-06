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
import { List, OrderedSet } from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';

import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import UnitsConfig from 'views/logic/aggregationbuilder/UnitsConfig';
import useViewsPlugin from 'views/test/testViewsPlugin';

import DataTableEntry from './DataTableEntry';

const f = (source: string, field: string = source): { field: string; source: string } => ({ field, source });
const createFields = (fields: Array<string>) => OrderedSet(fields.map((field) => f(field)));
const fields = createFields(['nf_dst_address', 'count()', 'max(timestamp)', 'card(timestamp)']);
const item = {
  nf_dst_address: '192.168.1.24',
  nf_proto_name: {
    TCP: { 'count()': 20, 'max(timestamp)': 1554106041841, 'card(timestamp)': 14 },
    UDP: { 'count()': 64, 'max(timestamp)': 1554106041841, 'card(timestamp)': 16 },
  },
  'count()': 84,
  'max(timestamp)': 1554106041841,
  'card(timestamp)': 20,
};

const columnPivots = ['nf_proto_name'];

const columnPivotValues = [['TCP'], ['UDP']];

const series = [
  Series.forFunction('count()'),
  Series.forFunction('max(timestamp)'),
  Series.forFunction('card(timestamp)'),
];

const valuePath = [{ nf_dst_address: '192.168.1.24' }];

const seriesWithName = (fn: string, name: string) =>
  Series.forFunction(fn).toBuilder().config(SeriesConfig.empty().toBuilder().name(name).build()).build();

jest.mock('views/hooks/useActiveQueryId', () => () => 'foobar');

const SUT = (props: Partial<React.ComponentProps<typeof DataTableEntry>>) => (
  <table>
    <tbody>
      <DataTableEntry
        columnPivots={columnPivots}
        columnPivotValues={columnPivotValues}
        fields={fields}
        item={item}
        series={series}
        types={List([])}
        valuePath={valuePath}
        units={UnitsConfig.empty()}
        {...props}
      />
    </tbody>
  </table>
);

describe('DataTableEntry', () => {
  useViewsPlugin();

  it('does not fail without types', async () => {
    render(<SUT />);
    await screen.findByText('192.168.1.24');
  });

  it('provides field types for fields and series', async () => {
    const types = [
      FieldTypeMapping.create('timestamp', FieldTypes.DATE()),
      FieldTypeMapping.create('nf_dst_address', FieldTypes.IP()),
    ];

    render(<SUT types={List(types)} />);

    await screen.findByText('192.168.1.24');

    expect(screen.getAllByText('2019-04-01 10:07:21.841')).toHaveLength(3);
  });

  it('provides valuePath in context for each value', async () => {
    render(<SUT />);

    const nestedValue = await screen.findByTestId(
      'value-cell-nf_dst_address:192.168.1.24-nf_proto_name:UDP-_exists_:timestamp-card(timestamp)',
    );

    expect(nestedValue).toHaveTextContent('16');
  });

  it('does not render `Empty Value` for deduplicated values', async () => {
    const fieldsWithDeduplicatedValues = createFields(['nf_dst_address', 'nf_dst_port']);
    const itemWithDeduplicatedValues = {
      nf_dst_port: 443,
    };
    render(<SUT fields={fieldsWithDeduplicatedValues} item={itemWithDeduplicatedValues} />);

    await screen.findByText(443);

    expect(screen.queryByText(/empty value/i)).not.toBeInTheDocument();
  });

  describe('resolves field types', () => {
    const timestampTypeMapping = FieldTypeMapping.create('timestamp', FieldTypes.DATE());

    it('for non-renamed functions', async () => {
      render(<SUT types={List([timestampTypeMapping])} columnPivots={[]} item={{ 'max(timestamp)': 1554106041841 }} />);

      await screen.findByText('2019-04-01 10:07:21.841');
    });

    it('for simple row with renamed function', async () => {
      const renamedSeries = [seriesWithName('max(timestamp)', 'Last Timestamp')];
      const itemWithRenamedSeries = {
        'Last Timestamp': 1554106041841,
      };
      const fieldsWithRenamedSeries = OrderedSet([f('max(timestamp)', 'Last Timestamp')]);

      render(
        <SUT
          columnPivots={[]}
          columnPivotValues={[]}
          fields={fieldsWithRenamedSeries}
          item={itemWithRenamedSeries}
          series={renamedSeries}
          types={List([timestampTypeMapping])}
          valuePath={[]}
        />,
      );

      const timestamp = await screen.findByTestId('value-cell--Last Timestamp');

      expect(timestamp).toHaveTextContent('2019-04-01 10:07:21.841');
    });
  });
});
