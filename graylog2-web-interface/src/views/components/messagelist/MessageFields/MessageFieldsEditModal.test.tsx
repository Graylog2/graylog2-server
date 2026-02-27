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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

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
import useMessageFavoriteFieldsForEditing from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFieldsForEditing';

import MessageFieldsEditModal from './MessageFieldsEditModal';

const reorderFavoriteFields = jest.fn();
const onFavoriteToggle = jest.fn();
const resetFavoriteFields = jest.fn();
const saveFavoriteFields = jest.fn();
const mockedToggleEditMode = jest.fn();

jest.mock('views/stores/StreamsStore', () => ({
  StreamsActions: { refresh: jest.fn() },
  StreamsStore: MockStore(['getInitialState', () => ({ streams: [{ id: 'streamId', title: 'Stream' }] })]),
}));

jest.mock('views/components/messagelist/MessageFields/hooks/useFormattedFields');
jest.mock('views/components/messagelist/MessageFields/hooks/useMessageFavoriteFieldsForEditing');
jest.mock('views/components/messagelist/MessageFields/hooks/useSendFavoriteFieldTelemetry', () => jest.fn);

const formattedFavorites = [
  {
    value: 'val1',
    type: FieldTypes.STRING(),
    field: 'fav1',
    id: 'fav1',
    title: 'fv1',
  },
];
const formattedRest = [
  {
    value: 'val2',
    type: FieldTypes.STRING(),
    field: 'rest1',
    id: 'rest1',
    title: 'rest1',
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
                saveFavoriteField: saveFavoriteFields,
                editableStreams: [],
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
    asMock(useMessageFavoriteFieldsForEditing).mockReturnValue({
      resetFavoriteFields,
      reorderFavoriteFields,
      onFavoriteToggle,
      editingFavoriteFields: ['fav1', 'fav2'],
      saveEditedFavoriteFields: saveFavoriteFields,
    });
  });

  it('renders headings', async () => {
    renderComponent();

    await screen.findByRole('heading', { name: /favorite fields configuration/i });
    await screen.findByRole('heading', { name: /^favorite fields$/i });
    await screen.findByRole('heading', { name: /remaining fields/i });
  });

  it('cancel button runs mockedToggleEditMode', async () => {
    renderComponent();
    const backToMessageButton = await screen.findByRole('button', { name: /cancel/i });
    userEvent.click(backToMessageButton);
    expect(mockedToggleEditMode).toHaveBeenCalled();
  });

  it('Reset fields call the context handlers', async () => {
    renderComponent();
    const resetFieldsButton = await screen.findByRole('button', { name: /reset to default/i });
    userEvent.click(resetFieldsButton);
    expect(resetFavoriteFields).toHaveBeenCalled();
    expect(mockedToggleEditMode).toHaveBeenCalled();
  });

  it('Save configuration call the context handlers', async () => {
    renderComponent();
    const saveFieldsButton = await screen.findByRole('button', { name: /save configuration/i });
    userEvent.click(saveFieldsButton);
    expect(saveFavoriteFields).toHaveBeenCalled();
    expect(mockedToggleEditMode).toHaveBeenCalled();
  });

  it('clicking favorite icon in Favorites calls onFavoriteToggle with field name', async () => {
    renderComponent();

    const removeIcon = await screen.findByTitle(/remove fav1 from favorites/i);

    await suppressConsole(() => userEvent.click(removeIcon));

    expect(onFavoriteToggle).toHaveBeenCalledWith('fav1');
  });

  it('clicking favorite icon in Details calls onFavoriteToggle with field name', async () => {
    await renderComponent();

    const removeIcon = await screen.findByTitle(/add rest1 to favorites/i);
    userEvent.click(removeIcon);
    expect(onFavoriteToggle).toHaveBeenCalledWith('rest1');
  });

  it('clicking reset to default calls resetFavoriteFields with field name', async () => {
    await renderComponent();

    const resetButton = await screen.findByRole('button', { name: /reset to default/i });
    userEvent.click(resetButton);
    expect(resetFavoriteFields).toHaveBeenCalledWith();
  });
});
