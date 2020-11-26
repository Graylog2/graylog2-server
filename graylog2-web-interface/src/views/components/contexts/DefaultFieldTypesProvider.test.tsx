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
