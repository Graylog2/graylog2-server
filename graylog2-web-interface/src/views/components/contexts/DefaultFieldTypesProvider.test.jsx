// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import { Map, List } from 'immutable';
import asMock from 'helpers/mocking/AsMock';
import { simpleFields, simpleQueryFields } from 'fixtures/fields';

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
    const fieldStoreState = { all: simpleFields(), queryFields: simpleQueryFields('queryId') };

    asMock(FieldTypesStore.getInitialState).mockReturnValue(fieldStoreState);

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(fieldStoreState);
  });
});
