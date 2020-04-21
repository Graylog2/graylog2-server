// @flow strict
import * as React from 'react';
import { cleanup, render } from 'wrappedTestingLibrary';
import { Map, List } from 'immutable';
import asMock from 'helpers/mocking/AsMock';

import { FieldTypesStore } from 'views/stores/FieldTypesStore';

import FieldTypesContext from './FieldTypesContext';
import DefaultFieldTypesProvider from './DefaultFieldTypesProvider';

const mockEmptyStore = { all: List(), queryFields: Map() };

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesStore: {
    listen: jest.fn(),
    getInitialState: jest.fn(() => mockEmptyStore),
  },
}));

describe('DefaultFieldTypesProvider', () => {
  afterEach(cleanup);

  const renderSUT = () => {
    const consume = jest.fn();
    render(
      <DefaultFieldTypesProvider>
        <FieldTypesContext.Consumer>
          {consume}
        </FieldTypesContext.Consumer>
      </DefaultFieldTypesProvider>,
    );
    return consume;
  };

  it('provides no field types with empty store', () => {
    asMock(FieldTypesStore.getInitialState).mockReturnValue(undefined);
    const consume = renderSUT();
    expect(consume).toHaveBeenCalledWith(undefined);
  });

  it('provides field types of field types store', () => {
    const _createField = (name) => ({ name, type: { type: 'string' } });
    const _createQueryFields = (fields) => ({ get: () => fields });
    const fieldsFixtures = ['source', 'message', 'timestamp'].map(_createField);
    const fieldStoreState = { all: fieldsFixtures, queryFields: _createQueryFields(fieldsFixtures) };

    asMock(FieldTypesStore.getInitialState).mockReturnValue(fieldStoreState);

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(fieldStoreState);
  });
});
