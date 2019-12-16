// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { mount } from 'wrappedEnzyme';

// $FlowFixMe: imports from core need to be fixed in flow
import mockComponent from 'helpers/mocking/MockComponent';

import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import Series from 'views/logic/aggregationbuilder/Series';
import DataTableEntry from './DataTableEntry';

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
});
