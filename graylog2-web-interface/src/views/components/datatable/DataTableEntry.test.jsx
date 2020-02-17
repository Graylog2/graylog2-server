// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { mount } from 'wrappedEnzyme';

import mockComponent from 'helpers/mocking/MockComponent';

import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import Series from 'views/logic/aggregationbuilder/Series';
import DataTableEntry from './DataTableEntry';
import SeriesConfig from '../../logic/aggregationbuilder/SeriesConfig';
import EmptyValue from '../EmptyValue';

jest.mock('views/components/common/UserTimezoneTimestamp', () => mockComponent('UserTimezoneTimestamp'));

const fields = Immutable.OrderedSet(['nf_dst_address', 'count()', 'max(timestamp)', 'card(timestamp)']);
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
  it('does not fail without types', () => {
    const wrapper = mount((
      <table>
        <DataTableEntry columnPivots={columnPivots}
                        columnPivotValues={columnPivotValues}
                        currentView={currentView}
                        fields={fields}
                        item={item}
                        series={series}
                        types={[]}
                        valuePath={valuePath} />
      </table>
    ));

    expect(wrapper).not.toBeEmptyRender();
    expect(wrapper).toMatchSnapshot();
  });

  it('provides field types for fields and series', () => {
    const types = [
      FieldTypeMapping.create('timestamp', FieldTypes.DATE()),
      FieldTypeMapping.create('nf_dst_address', FieldTypes.STRING()),
    ];

    const wrapper = mount((
      <table>
        <DataTableEntry columnPivots={columnPivots}
                        columnPivotValues={columnPivotValues}
                        currentView={currentView}
                        fields={fields}
                        item={item}
                        series={series}
                        types={types}
                        valuePath={valuePath} />
      </table>
    ));

    const fieldTypeFor = fieldName => wrapper.find(`Value[field="${fieldName}"]`).first().props().type;
    expect(fieldTypeFor('nf_dst_address')).toEqual(FieldTypes.STRING());
    expect(fieldTypeFor('count()')).toEqual(FieldTypes.LONG());
    expect(fieldTypeFor('max(timestamp)')).toEqual(FieldTypes.DATE());
    expect(fieldTypeFor('card(timestamp)')).toEqual(FieldTypes.LONG());
  });

  it('provides valuePath in context for each value', () => {
    const wrapper = mount((
      <table>
        <DataTableEntry columnPivots={columnPivots}
                        columnPivotValues={columnPivotValues}
                        currentView={currentView}
                        fields={fields}
                        item={item}
                        series={series}
                        types={[]}
                        valuePath={valuePath} />
      </table>
    ));
    expect(wrapper.find('Provider')
      .map(p => p.props().value))
      .toMatchSnapshot();
  });

  it('does not render `Empty Value` for deduplicated values', () => {
    const fieldsWithDeduplicatedValues = Immutable.OrderedSet(['nf_dst_address', 'nf_dst_port']);
    const itemWithDeduplicatedValues = {
      nf_dst_port: 443,
    };
    const wrapper = mount((
      <table>
        <DataTableEntry columnPivots={columnPivots}
                        columnPivotValues={columnPivotValues}
                        currentView={currentView}
                        fields={fieldsWithDeduplicatedValues}
                        item={itemWithDeduplicatedValues}
                        series={series}
                        types={[]}
                        valuePath={valuePath} />
      </table>
    ));
    expect(wrapper).not.toContainReact(<EmptyValue />);
  });

  describe('resolves field types', () => {
    const timestampTypeMapping = FieldTypeMapping.create('timestamp', FieldTypes.DATE());
    it('for non-renamed functions', () => {
      const wrapper = mount((
        <table>
          <DataTableEntry columnPivots={columnPivots}
                          columnPivotValues={columnPivotValues}
                          currentView={currentView}
                          fields={fields}
                          item={item}
                          series={series}
                          types={[timestampTypeMapping]}
                          valuePath={valuePath} />
        </table>
      ));
      const valueFields = wrapper.find('Value[field="max(timestamp)"]');
      valueFields.forEach(field => expect(field).toHaveProp('type', timestampTypeMapping.type));
    });
    it('for simple row with renamed function', () => {
      const renamedSeries = [seriesWithName('max(timestamp)', 'Last Timestamp')];
      const itemWithRenamedSeries = {
        'Last Timestamp': 1554106041841,
      };
      const fieldsWithRenamedSeries = Immutable.OrderedSet(['Last Timestamp']);

      const wrapper = mount((
        <table>
          <DataTableEntry columnPivots={[]}
                          columnPivotValues={[]}
                          currentView={currentView}
                          fields={fieldsWithRenamedSeries}
                          item={itemWithRenamedSeries}
                          series={renamedSeries}
                          types={[timestampTypeMapping]}
                          valuePath={[]} />
        </table>
      ));
      const valueFields = wrapper.find('Value[field="Last Timestamp"]');
      expect(valueFields).toHaveLength(1);
      valueFields.forEach(field => expect(field).toHaveProp('type', timestampTypeMapping.type));
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
      const fieldsWithRenamedSeries = Immutable.OrderedSet(['nf_dst_address', 'count()', 'Last Timestamp', 'card(timestamp)']);

      const wrapper = mount((
        <table>
          <DataTableEntry columnPivots={columnPivots}
                          columnPivotValues={columnPivotValues}
                          currentView={currentView}
                          fields={fieldsWithRenamedSeries}
                          item={itemWithRenamedSeries}
                          series={renamedSeries}
                          types={[timestampTypeMapping]}
                          valuePath={valuePath} />
        </table>
      ));
      const valueFields = wrapper.find('Value[field="Last Timestamp"]');
      expect(valueFields).toHaveLength(3);
      valueFields.forEach(field => expect(field).toHaveProp('type', timestampTypeMapping.type));
    });
  });
});
