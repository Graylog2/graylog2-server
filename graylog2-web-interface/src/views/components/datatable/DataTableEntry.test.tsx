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
import { mount } from 'wrappedEnzyme';
import mockComponent from 'helpers/mocking/MockComponent';

import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';

import DataTableEntry from './DataTableEntry';

import EmptyValue from '../EmptyValue';

jest.mock('views/components/common/UserTimezoneTimestamp', () => mockComponent('UserTimezoneTimestamp'));

const f = (source: string, field: string = source): { field: string, source: string } => ({ field, source });
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
const currentView = { activeQuery: '6ca0ea05-6fc1-4f46-9b22-20a5baab7b0d' };

const series = [
  Series.forFunction('count()'),
  Series.forFunction('max(timestamp)'),
  Series.forFunction('card(timestamp)'),
];

const valuePath = [{ nf_dst_address: '192.168.1.24' }];

const seriesWithName = (fn, name) => Series.forFunction(fn)
  .toBuilder()
  .config(SeriesConfig.empty()
    .toBuilder()
    .name(name)
    .build())
  .build();

describe('DataTableEntry', () => {
  const SUT = (props) => (
    <table>
      <DataTableEntry columnPivots={columnPivots}
                      columnPivotValues={columnPivotValues}
                      currentView={currentView}
                      fields={fields}
                      item={item}
                      series={series}
                      types={List([])}
                      valuePath={valuePath}
                      {...props} />
    </table>
  );

  it('does not fail without types', () => {
    const wrapper = mount((
      <SUT />
    ));

    expect(wrapper).not.toBeEmptyRender();
    expect(wrapper).toExist();
  });

  it('provides field types for fields and series', () => {
    const types = [
      FieldTypeMapping.create('timestamp', FieldTypes.DATE()),
      FieldTypeMapping.create('nf_dst_address', FieldTypes.STRING()),
    ];

    const wrapper = mount((
      <SUT types={List(types)} />
    ));

    const fieldTypeFor = (fieldName) => wrapper.find(`Value[field="${fieldName}"]`).first().props().type;

    expect(fieldTypeFor('nf_dst_address')).toEqual(FieldTypes.STRING());
    expect(fieldTypeFor('count()')).toEqual(FieldTypes.LONG());
    expect(fieldTypeFor('max(timestamp)')).toEqual(FieldTypes.DATE());
    expect(fieldTypeFor('card(timestamp)')).toEqual(FieldTypes.LONG());
  });

  it('provides valuePath in context for each value', () => {
    const wrapper = mount((
      <SUT />
    ));

    expect(wrapper.find('Provider')
      .map((p) => p.props().value))
      .toMatchSnapshot();
  });

  it('does not render `Empty Value` for deduplicated values', () => {
    const fieldsWithDeduplicatedValues = createFields(['nf_dst_address', 'nf_dst_port']);
    const itemWithDeduplicatedValues = {
      nf_dst_port: 443,
    };
    const wrapper = mount((
      <SUT fields={fieldsWithDeduplicatedValues}
           item={itemWithDeduplicatedValues} />
    ));

    expect(wrapper).not.toContainReact(<EmptyValue />);
  });

  describe('resolves field types', () => {
    const timestampTypeMapping = FieldTypeMapping.create('timestamp', FieldTypes.DATE());

    it('for non-renamed functions', () => {
      const wrapper = mount((
        <SUT types={List([timestampTypeMapping])} />
      ));
      const valueFields = wrapper.find('Value[field="max(timestamp)"]');

      valueFields.forEach((field) => expect(field).toHaveProp('type', timestampTypeMapping.type));
    });

    it('for simple row with renamed function', () => {
      const renamedSeries = [seriesWithName('max(timestamp)', 'Last Timestamp')];
      const itemWithRenamedSeries = {
        'Last Timestamp': 1554106041841,
      };
      const fieldsWithRenamedSeries = OrderedSet([f('max(timestamp)', 'Last Timestamp')]);

      const wrapper = mount((
        <SUT columnPivots={[]}
             columnPivotValues={[]}
             fields={fieldsWithRenamedSeries}
             item={itemWithRenamedSeries}
             series={renamedSeries}
             types={List([timestampTypeMapping])}
             valuePath={[]} />
      ));
      const valueFields = wrapper.find('Value[field="max(timestamp)"]');

      expect(valueFields).toHaveLength(1);

      valueFields.forEach((field) => expect(field).toHaveProp('type', timestampTypeMapping.type));
    });

    it('for renamed functions', () => {
      const renamedSeries = [
        Series.forFunction('count()'),
        seriesWithName('max(timestamp)', 'Last Timestamp'),
        Series.forFunction('card(timestamp)'),
      ];
      const itemWithRenamedSeries = {
        nf_dst_address: '192.168.1.24',
        nf_proto_name: {
          TCP: { 'count()': 20, 'Last Timestamp': 1554106041841, 'card(timestamp)': 14 },
          UDP: { 'count()': 64, 'Last Timestamp': 1554106041841, 'card(timestamp)': 16 },
        },
        'count()': 84,
        'Last Timestamp': 1554106041841,
        'card(timestamp)': 20,
      };
      const fieldsWithRenamedSeries = OrderedSet([
        f('nf_dst_address'),
        f('count()'),
        f('max(timestamp)', 'Last Timestamp'),
        f('card(timestamp)'),
      ]);

      const wrapper = mount((
        <SUT fields={fieldsWithRenamedSeries}
             item={itemWithRenamedSeries}
             series={renamedSeries}
             types={List([timestampTypeMapping])} />
      ));
      const valueFields = wrapper.find('Value[field="max(timestamp)"]');

      expect(valueFields).toHaveLength(3);

      valueFields.forEach((field) => expect(field).toHaveProp('type', timestampTypeMapping.type));
    });
  });
});
