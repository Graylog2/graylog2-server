// @flow strict
import React from 'react';
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';

import PivotSelect from './PivotSelect';
import PivotGenerator from '../../logic/searchtypes/aggregation/PivotGenerator';
import FieldType from '../../logic/fieldtypes/FieldType';
import FieldTypeMapping from '../../logic/fieldtypes/FieldTypeMapping';

jest.mock('stores/connect', () => x => x);
jest.mock('views/stores/FieldTypesStore', () => ({}));
jest.mock('views/logic/searchtypes/aggregation/PivotGenerator', () => jest.fn());

describe('PivotSelect', () => {
  it('renders properly with minimal parameters', () => {
    const wrapper = mount(<PivotSelect onChange={() => {}} fields={Immutable.List()} value={[]} />);
    expect(wrapper).not.toBeEmptyRender();
  });
  describe('upon pivot list change, field types are passed for new pivot generation', () => {
    it('using Unknown if field is not found', () => {
      const wrapper = mount(<PivotSelect onChange={() => {}} fields={{ all: Immutable.List() }} value={[]} />);
      const cb = wrapper.find('SortableSelect').at(0).props().onChange;

      cb([{ value: 'foo' }]);

      expect(PivotGenerator).toHaveBeenCalledWith('foo', FieldType.Unknown);
    });
    it('using field type found in fields list', () => {
      const fieldType = new FieldType('keyword', [], []);
      const fieldTypeMapping = new FieldTypeMapping('foo', fieldType);
      const fields = Immutable.List([
        fieldTypeMapping,
      ]);
      const wrapper = mount(<PivotSelect onChange={() => {}} fields={{ all: fields }} value={[]} />);
      const cb = wrapper.find('SortableSelect').at(0).props().onChange;

      cb([{ value: 'foo' }]);

      expect(PivotGenerator).toHaveBeenCalledWith('foo', fieldType);
    });
  });
});
