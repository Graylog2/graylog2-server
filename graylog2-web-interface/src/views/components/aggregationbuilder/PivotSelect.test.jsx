// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';
import { simpleFields, simpleQueryFields } from 'fixtures/fields';
import suppressConsole from 'helpers/suppressConsole';

import PivotGenerator from 'views/logic/searchtypes/aggregation/PivotGenerator';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypesContext, { type FieldTypes } from 'views/components/contexts/FieldTypesContext';

import PivotSelect from './PivotSelect';

jest.mock('stores/connect', () => (x) => x);
jest.mock('views/stores/FieldTypesStore', () => ({}));
jest.mock('views/logic/searchtypes/aggregation/PivotGenerator', () => jest.fn());

describe('PivotSelect', () => {
  const initialFieldTypes = { all: simpleFields(), queryFields: simpleQueryFields('queryId') };
  type SimplePivotProps = {
    fieldTypes?: ?FieldTypes,
  };
  const SimplePivotSelect = ({ fieldTypes }: SimplePivotProps) => (
    <FieldTypesContext.Provider value={fieldTypes}>
      <PivotSelect onChange={() => {}} value={[]} />
    </FieldTypesContext.Provider>
  );

  SimplePivotSelect.defaultProps = {
    fieldTypes: initialFieldTypes,
  };

  it('renders properly with minimal parameters', () => {
    const wrapper = mount(<SimplePivotSelect />);

    expect(wrapper).not.toBeEmptyRender();
  });

  it('renders properly with `undefined` fields', () => {
    suppressConsole(() => {
      const wrapper = mount(<PivotSelect onChange={() => {}} value={[]} />);

      expect(wrapper).not.toBeEmptyRender();
    });
  });

  describe('upon pivot list change, field types are passed for new pivot generation', () => {
    it('using Unknown if field is not found', () => {
      const wrapper = mount(<SimplePivotSelect fieldTypes={{ ...initialFieldTypes, all: Immutable.List() }} />);
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
      const wrapper = mount(<SimplePivotSelect fieldTypes={{ ...initialFieldTypes, all: fields }} />);
      const cb = wrapper.find('SortableSelect').at(0).props().onChange;

      cb([{ value: 'foo' }]);

      expect(PivotGenerator).toHaveBeenCalledWith('foo', fieldType);
    });
  });
});
