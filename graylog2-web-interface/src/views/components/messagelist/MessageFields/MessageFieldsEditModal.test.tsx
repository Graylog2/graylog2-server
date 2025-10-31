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

import React from 'react';
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import { StoreMock as MockStore, asMock } from 'helpers/mocking';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import TestStoreProvider from 'views/test/TestStoreProvider';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import useViewsPlugin from 'views/test/testViewsPlugin';
import suppressConsole from 'helpers/suppressConsole';
import useFormattedFields from 'views/components/messagelist/MessageFields/hooks/useFormattedFields';

import MessageFieldsEditModal from './MessageFieldsEditModal';

const removeFromFavoriteFields = jest.fn();
const addToFavoriteFields = jest.fn();
const resetFavoriteField = jest.fn();
const saveFavoriteFields = jest.fn();
const cancelEdit = jest.fn();
const mockedToggleEditMode = jest.fn();

jest.mock('views/stores/StreamsStore', () => ({
  StreamsActions: { refresh: jest.fn() },
  StreamsStore: MockStore(['getInitialState', () => ({ streams: [{ id: 'streamId', title: 'Stream' }] })]),
}));

jest.mock('views/components/messagelist/MessageFields/hooks/useFormattedFields');

const formattedFavorites = [
  {
    value: 'val1',
    type: FieldTypes.STRING(),
    field: 'fav1',
    id: 'fav1',
  },
];
const formattedRest = [
  {
    value: 'val2',
    type: FieldTypes.STRING(),
    field: 'rest1',
    id: 'rest1',
  },
];
const message = {
  id: 'id',
  index: 'index',
  fields: { streams: ['streamId'] },
  formatted_fields: ['foo', 'rest1'],
} as any;
const search = Search.builder().id('search-id').build();

const view = View.builder().search(search).build();

const renderComponent = () =>
  suppressConsole(() =>
    render(
      <TestStoreProvider view={view}>
        <GraylogThemeProvider userIsLoggedIn>
          <MessageFavoriteFieldsContext.Provider
            value={
              {
                removeFromFavoriteFields,
                addToFavoriteFields,
                resetFavoriteField,
                saveFavoriteFields,
                cancelEdit,
                message,
              } as any
            }>
            <MessageFieldsEditModal toggleEditMode={mockedToggleEditMode} />
          </MessageFavoriteFieldsContext.Provider>
        </GraylogThemeProvider>
      </TestStoreProvider>,
    ),
  );

describe('MessageFieldsEditMode (integration, real components)', () => {
  useViewsPlugin();

  beforeEach(() => {
    asMock(useFormattedFields).mockReturnValue({ formattedRest, formattedFavorites });
  });

  it('renders headings', async () => {
    renderComponent();

    await screen.findByRole('heading', { name: /favorites/i });
    await screen.findByRole('heading', { name: /details/i });
  });

  it('cancel button calls cancelEdit', async () => {
    renderComponent();
    const backToMessageButton = await screen.findByRole('button', { name: /cancel/i });
    fireEvent.click(backToMessageButton);
    expect(cancelEdit).toHaveBeenCalled();
    expect(mockedToggleEditMode).toHaveBeenCalled();
  });

  it('Reset fields call the context handlers', async () => {
    renderComponent();
    const resetFieldsButton = await screen.findByRole('button', { name: /reset fields/i });
    fireEvent.click(resetFieldsButton);
    expect(resetFavoriteField).toHaveBeenCalled();
    expect(mockedToggleEditMode).toHaveBeenCalled();
  });

  it('Save configuration call the context handlers', async () => {
    renderComponent();
    const saveFieldsButton = await screen.findByRole('button', { name: /save configuration/i });
    fireEvent.click(saveFieldsButton);
    expect(saveFavoriteFields).toHaveBeenCalled();
    expect(mockedToggleEditMode).toHaveBeenCalled();
  });

  it('clicking favorite icon in Favorites calls removeFromFavoriteFields with field name', async () => {
    renderComponent();

    const removeIcon = await screen.findByTitle(/remove fav1 from favorites/i);

    await suppressConsole(() => fireEvent.click(removeIcon));

    expect(removeFromFavoriteFields).toHaveBeenCalledWith('fav1');
  });

  test('clicking favorite icon in Details calls addToFavoriteFields with field name', async () => {
    await renderComponent();

    const removeIcon = await screen.findByTitle(/add rest1 to favorites/i);
    fireEvent.click(removeIcon);
    expect(addToFavoriteFields).toHaveBeenCalled();
    expect(addToFavoriteFields).toHaveBeenCalledWith('rest1');
  });
});
